# mapservice-rest — 시설물 관리 플랫폼의 지도·시설물 API

> 현장조사(QField) → 동기화 → **이 API** → 지도 화면으로 이어지는 시설물 관리 플랫폼의 백엔드입니다.
> 동시에 8개 저장소로 나뉜 이 시스템의 **총괄 문서·배포 스크립트·작업 이력**을 관리하는 기준 저장소이기도 합니다.

| | |
|---|---|
| **데모** | https://sj-lab.co.kr → "시설물 관리" (로그인 화면의 **체험용 계정 버튼**으로 바로 입장) |
| **API** | `https://api.sj-lab.co.kr/map/...` (게이트웨이 경유) |
| **스택** | Java 17 · Spring Boot 3.3.2 · Spring Cloud(Eureka) · MyBatis · PostgreSQL 17 + PostGIS 3.4 · OpenPDF |
| **배포** | Jenkins → NCP 레지스트리 → ArgoCD(GitOps) → Kubernetes |

---

## 1. 무엇을 푸는 서비스인가

재난·공공시설물을 **현장에서 조사하고(모바일), 사무실에서 처리하는(웹)** 한 사이클을 다룹니다.

```
[현장] QField 포크 앱으로 시설물 점검·사진·음성 메모
   │ 업로드
[동기화] sj-qfieldsync 워커가 30초마다 GPKG → PostGIS 적재
   │
[이 서비스] 시설물·행정구역·공공데이터를 GeoJSON으로 제공, 내업(사무실 처리) 기록 저장
   │
[웹] 지도에서 조회 → 보수 필요 시설물에 처리 내역·사진 등록 → PDF 보고서 출력
```

제가 맡은 범위는 **DB 설계부터 백엔드 API, 프론트 연동, 쿠버네티스 배포, 운영 대응까지 전 구간**입니다.

---

## 2. 면접에서 봐주셨으면 하는 부분

### ① GeoJSON을 DB에서 조립 — 애플리케이션은 문자열만 전달

지도 서비스는 응답 크기가 곧 성능입니다. 그래서 좌표 변환·중복 제거·속성 구성을 **모두 PostGIS SQL에서 끝내고**, 애플리케이션은 완성된 GeoJSON 문자열을 그대로 흘려보냅니다(DTO 매핑·직렬화 비용 제거).

```sql
-- resources/mapper/qfield-facility.xml (발췌)
json_build_object(
  'type', 'Feature',
  'geometry', ST_AsGeoJSON(f.geom)::json,
  'properties', json_build_object('total_id', f.total_id, ...)
)
```

읽기 전용 DB라 뷰를 만들 수 없는 시설물 쪽은 XML SQL에서 직접 조립하고, 뷰를 만들 수 있는 공공데이터 레이어는 배치(`sj-lab-scheduler`)가 만든 `map.v_*_geojson` 뷰를 그대로 select합니다.

### ② 200MB 응답 → 화면 영역 단위 조회로 재설계

전국 데이터를 통째로 내려주던 구조에서 **버스정류장 85MB·병원 61MB**가 나와 최초 표출에 수십 초가 걸렸습니다.

- `bbox`(EPSG:3857) + `limit` 파라미터를 추가하고, bbox가 있으면 뷰가 아니라 **원본 테이블**을 조회
- 상한을 넘으면 bbox를 격자로 나눠 **칸마다 한 건씩(`row_number`) 뽑아** 화면 전체에 고르게 퍼진 표본을 반환 (단순 `limit`이면 화면 한쪽만 채워집니다)
- 응답 gzip 압축(실측 10배 이상 축소), `Cache-Control` 차등 적용(시설물 60초 / WFS 300초 / 행정구역 3600초)
- 구버전 프론트 호환을 위해 **bbox 없이 호출하면 기존 전국 응답**을 유지

### ③ 공간 쿼리 최적화 — 인덱스를 타게 만드는 조인

시설물(EPSG:3857 점)과 행정구역(EPSG:4326 폴리곤)을 조인할 때, 폴리곤 쪽을 변환하면 GIST 인덱스를 못 탑니다.

```sql
LEFT JOIN LATERAL (
  SELECT emd_cd FROM public.g_emd e
  WHERE ST_Intersects(e.geom, ST_Transform(f.geom, 4326))   -- 점만 변환
  LIMIT 1
) e ON true
```

행정구역 필터는 코드의 **접두어 계층**(시도 2자리 → 시군구 5자리 → 읍면동 8자리)을 이용해 `emd_cd LIKE :code || '%'` 한 조건으로 처리합니다. BBOX 계산도 폴리곤 전체가 아니라 `ST_Transform(ST_Envelope(geom), 3857)`로 외곽만 변환합니다.

### ④ "테이블이 없어도 지도는 떠야 한다" — 단계적 기능 저하

내업 기능은 DDL을 담당자가 직접 실행하는 환경이라, **스크립트 실행 전에 배포되는 순간**이 반드시 생깁니다.

- 테이블 존재 여부를 `to_regclass`로 확인해 캐시하고, 없으면 60초마다 재확인 → **스크립트 실행 후 재기동 없이 반영**
- 있다고 캐시한 뒤 조회가 `42P01`로 실패하면 즉시 폴백 쿼리로 전환
- 첫 쿼리 실패가 폴백까지 말아 올리지 않도록 목록 조회는 `Propagation.NOT_SUPPORTED`
- 결과: 내업 테이블이 없어도 **시설물 목록은 정상 동작**(보수 필요 건은 `PENDING`으로 표시)

### ⑤ 외부 인증이 필요한 첨부 파일 중계

현장 사진·음성·영상은 QFieldCloud에 있고 API가 인증을 요구해 브라우저가 직접 받을 수 없습니다. 백엔드가 토큰 로그인(6시간 캐시) → 프로젝트 식별(캐시) → 파일 다운로드를 대신합니다.

- **요청된 경로가 그 시설물의 첨부인지 DB로 검증한 뒤에만 전달**(아니면 403) — 임의 파일 접근 차단
- 원본이 내려주는 `application.force-download`를 확장자 기반 실제 MIME으로 교정해 브라우저가 재생할 수 있게 함
- 계정은 환경변수·k8s Secret으로만 주입하고, 없으면 **이 엔드포인트만 503**(나머지 기능은 정상)

### ⑥ 업로드 검증과 동시성

내업 사진 업로드는 구분(전/후)별 5장·장당 10MB·JPEG/PNG/WebP 제한인데,

- `Content-Type`·확장자뿐 아니라 **파일 시그니처(매직 넘버)로 실제 이미지인지 확인**(헤더만 위조한 파일 차단)
- 장수 한도는 내업 기록 행을 `FOR UPDATE`로 잠근 뒤 세어 **동시 업로드에서도 초과되지 않게** 처리
- multipart 한도 초과는 컨트롤러 진입 전에 발생하므로 `@ControllerAdvice`에서 413 JSON으로 변환하고, 본문을 끝까지 읽도록 `server.tomcat.max-swallow-size`를 20MB로 조정(기본 2MB면 연결이 끊겨 413이 전달되지 않음)

---

## 3. 시스템 구조

```
[현장조사 앱] infra-manage-app(QField 포크) → QFieldCloud
        │ 30초 주기 동기화
[워커] sj-qfieldsync ─────────────┐
[배치] sj-lab-scheduler ──────────┤ 적재
                                  ▼
                    [DB] PostgreSQL 17 + PostGIS 3.4
                                  │ 조회(MyBatis)
[프론트] sj-lab-mapservice ─▶ [게이트웨이] sj-lab-apigateway ─▶ [이 서비스]
[허브] sj-lab-hub          ─▶        :8100                         │
[로그인] sj-lab-authserver ◀─────────┘         [Eureka] sj-lab-discoveryServer
```

| 저장소 | 역할 |
|---|---|
| `sj-lab-mapservice` | 지도 프론트엔드(정적 SPA, OpenLayers) |
| `sj-lab-hub` | 첫 화면(React) |
| `sj-lab-apigateway` | 라우팅·CORS (Spring Cloud Gateway) |
| `sj-lab-discoveryServer` | 서비스 레지스트리(Eureka) |
| `sj-lab-authserver` | QFieldCloud 계정 위임 로그인 + JWT + SSO 로그인 페이지 |
| `sj-lab-scheduler` | 공공데이터 수집 배치 + 지도용 뷰 생성 |
| `sj-qfieldsync` | QFieldCloud → PostGIS 동기화 워커(Python) |
| `sj-lab-k8s-manifests` | 서비스별 Helm 차트(ArgoCD GitOps 소스) |

---

## 4. API 요약

### 공공데이터 WFS 레이어
`GET /map/convenience-store` · `busStop-info` · `cctv-info` · `pharmacy-info` · `hospital-info` · `governmentOffice-info`
→ 공통 파라미터 `bbox`(EPSG:3857), `limit`(기본 3000, 최대 20000)

### 시설물·행정구역
| 엔드포인트 | 설명 |
|---|---|
| `GET /map/qfield/facilities?sidoCd=&sggCd=&emdCd=` | 시설물 목록(보수 필요 여부·내업 상태 포함) |
| `GET /map/qfield/facilities/{totalId}` | 상세 |
| `GET /map/qfield/facilities/{totalId}/media?path=` | 첨부 중계(사진·음성·영상) |
| `GET /map/qfield/facility-icons` | 아이콘 설정(DB 기준) |
| `GET /map/admin-area/sido` · `sgg?sidoCd=` · `emd?sggCd=` | 행정구역 |

### 내업(쓰기 API)
| 엔드포인트 | 설명 |
|---|---|
| `GET/POST /map/qfield/facilities/{totalId}/office-works` | 조회·등록(**보수 필요 시설물만**, 아니면 400) |
| `PUT/DELETE /map/qfield/office-works/{workId}` | 수정 / 소프트 삭제 |
| `POST/GET/DELETE .../office-works/{workId}/photos[/{photoId}]` | 처리 전·후 사진 |
| `GET /map/qfield/facilities/{totalId}/report/pdf?workId=` | 내업 보고서 PDF(완료·보류 기록만) |

상태 코드는 검증 실패 400 / 미존재 404 / 중복 409 / 용량 초과 413 / 계정 미설정 503으로 구분합니다. 전체 계약은 `docs/system-architecture.md`의 API 표에 정리돼 있습니다.

---

## 5. 실행

```bash
mvnw.cmd clean package                                     # target/sj-lab-mapservice-rest.jar
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
mvnw.cmd test
```

전체 스택(Eureka → 백엔드 → 로그인 서버 → 게이트웨이 → 프론트)을 한 번에:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 start
powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 status
powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 stop
```

### 환경변수

| 변수 | 용도 | 없으면 |
|---|---|---|
| `QFIELD_USERNAME` / `QFIELD_PASSWORD` | 첨부 중계용 QFieldCloud 계정 | 첨부 엔드포인트만 503 |
| `QFIELD_BASE_URL` | 기본값 `https://qfield.sj-lab.co.kr` | — |

저장소는 public이므로 값은 파일에 적지 않습니다. 로컬은 git 제외 파일에서 스크립트가 주입하고, 운영은 k8s Secret(`qfield-credentials`)에서 받습니다.

### DB 스크립트

| 스크립트 | 없을 때 |
|---|---|
| `db/map_facility_icon.sql` | API가 빈 배열 → 프론트 내장 아이콘으로 동작 |
| `db/map_facility_office_work.sql` | 시설물 목록은 폴백 동작, 내업 API만 500 |
| `db/map_facility_office_work_photo.sql` | 사진 API만 500 |

앱이 쓰는 테이블은 **반드시 `map` 스키마**에 둡니다 — `qfield` 스키마는 동기화 워커가 관리하며, 프로젝트 패턴이 아닌 테이블을 아카이브 후 DROP하기 때문입니다(실제로 두 번 겪고 이전했습니다).

---

## 6. 운영에서 겪은 문제와 조치 (`history/`, `docs/`)

| 사례 | 원인 | 조치 |
|---|---|---|
| **배포하면 지도 사이트가 사라짐** | 허브 배포 잡의 `cleanRemote: true` 대상이 최종 웹 디렉터리라, 하위 폴더인 지도(`html/map`)까지 삭제. nginx SPA 폴백 때문에 404가 아니라 허브 화면이 200으로 떠 증상이 가려짐 | 스테이징 경유 + `rsync --delete --exclude 'map/'`로 교체, 배포 후 검증 스테이지·점검 스크립트 추가 (`docs/deploy-static-sites.md`) |
| **로그인 게이트 배포 순서 사고** | 로그인 서버가 뜨기 전에 프론트 게이트를 먼저 배포해 전체 사이트 503 | 즉시 revert 후 순서 정립(서버 배포·검증 → 게이트 적용) |
| **로그아웃이 안 됨** | 프론트는 localStorage만 지웠는데 로그인 페이지의 세션 쿠키가 살아 있어 즉시 재발급 | 로그아웃 시 `POST /auth/logout`으로 쿠키부터 제거 |
| **체험 계정 비밀번호 노출** | 데모 로그인을 페이지 JS에 계정을 담아 구현 | 서버 엔드포인트(`POST /auth/login/demo`) + Secret으로 이전, 비밀번호 교체 |
| **롤아웃 중 간헐 500/503** | 옛 인스턴스가 Eureka에 남아 있는 구간 | 원인·복구 절차 문서화(`docs/dev-environment.md`), 무중단 롤아웃은 개선 과제로 정리 |

작업 이력은 `history/history_v*.md`에 버전별로 남기고, 팀 공유용 페이지(`history/web/`)로도 발행합니다.

---

## 7. 문서

| 문서 | 내용 |
|---|---|
| `docs/system-architecture.md` | 전체 구조, **API 계약 표**, 저장소 간 변경 체크리스트, 배포 경로 |
| `docs/dev-environment.md` | 로컬 경로·포트(8100 게이트웨이 / 8761 Eureka / 4000 프론트)·CORS·기동 순서 |
| `docs/deploy-static-sites.md` | 정적 사이트 배포 구조와 "지도가 지워지는 문제" 원인·조치 |
| `docs/jenkins/*.groovy` | 허브·지도 배포 파이프라인(교체본 + 수정 전 원본) |
| `docs/k8s-secrets.md` | Secret·Jenkins Credential 이름·용도·확인 명령(값 없음) |
| `docs/analysis/*.md` | 개발 DB 점검·인덱스·데이터 품질 분석 |
| `scripts/check-prod-sites.ps1` | 운영 허브·지도가 각자 제 파일로 서빙되는지 점검 |

---

## 8. 현재 한계와 다음 과제

솔직하게 적어 둡니다.

- **백엔드 API에 토큰 검증이 없습니다.** 화면은 SSO 게이트로 막았지만 API는 열려 있어, 게이트웨이 전역 JWT 필터 + 역할 분리(체험 계정 읽기 전용)가 다음 우선순위입니다.
- 테스트가 컨텍스트 로딩 스모크 수준입니다. Testcontainers(PostGIS)로 매퍼 XML의 공간 쿼리를 검증하는 것을 준비 중입니다.
- `WfsController`가 예외를 삼키고 200 + 빈 바디를 반환하는 구간이 남아 있습니다(표준 에러 응답으로 교체 예정).
- 관측성(Actuator + Prometheus/Grafana)과 무중단 롤아웃(`replicas: 2`, preStop)이 없습니다.

작업 규칙(커밋·DB·파일 삭제·기록)은 `CLAUDE.md`에 정리돼 있습니다.
