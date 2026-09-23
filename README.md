# mapservice-rest — 지도 및 시설물 관리 GeoJSON API 서비스

`mapservice-rest`는 공간정보(GIS) 데이터와 현장 시설물 점검 데이터를 처리하여 클라이언트(웹 지도 SPA)에 고성능 GeoJSON으로 제공하는 Spring Boot 기반 마이크로서비스입니다. 현장조사(모바일)에서 수집된 공간 객체와 행정구역, 공공데이터를 유기적으로 결합하고, 시설물에 대한 사무실 후속 조치(내업 기록 및 현장 사진 관리)를 지원합니다.

---

## 1. 서비스 역할 및 핵심 책임

- **GIS 공간 데이터 서빙**: PostGIS 공간 테이블 및 뷰를 기반으로 전국 단위 공공데이터(CCTV, 버스정류장, 병원, 약국, 편의점, 관공서)와 QField 현장 점검 시설물을 GeoJSON FeatureCollection 형태로 제공합니다.
- **화면 영역(BBOX) 기반 동적 표본화**: 수십만 건의 대용량 공간 데이터를 웹 클라이언트 뷰포트에 맞게 실시간 필터링하고 그리드 표본화하여 네트워크 부하를 최소화합니다.
- **시설물 내업(사무실 조치) 및 미디어 관리**: 현장 점검 결과 보수가 필요한 시설물에 대한 조치 이력 등록, 변경 상태 관리, 전/후 증빙 사진 업로드 및 QFieldCloud 미디어 중계를 수행합니다.
- **마이크로서비스 아키텍처 연동**: Spring Cloud Netflix Eureka에 인스턴스를 자동 등록하고, Spring Cloud Gateway 뒤에서 서비스 경로(`/map/**`)를 처리합니다.

---

## 2. 기술 스택

- **언어 및 런타임**: Java 17, Spring Boot 3.3.2
- **데이터베이스 및 공간 엔진**: PostgreSQL 17, PostGIS 3.4 (MyBatis 3.0 매퍼 연동)
- **클라우드 및 MSA**: Spring Cloud Netflix Eureka Client, Spring Cloud Gateway 연동
- **문서화 및 유틸리티**: SpringDoc OpenAPI (Swagger 3), OpenPDF
- **배포 환경**: Docker, Kubernetes, Helm, Jenkins CI, ArgoCD (GitOps)

---

## 3. 시스템 아키텍처 및 업무 프로세스

### 3.1 전체 데이터 및 서비스 흐름

```
[현장조사 (infra-manage-app)] ──(동기화)──> [QFieldCloud]
                                              │
                                              ▼ (30초 주기 동기화: sj-qfieldsync)
                                     [PostgreSQL / PostGIS]
                                       ├─ qfield.facility_total_view (현장 시설물)
                                       ├─ map.v_*_geojson (공공 수집 데이터)
                                       ├─ map.facility_office_work (내업 기록)
                                       └─ public.g_emd / g_sgg (행정구역 경계)
                                              ▲
                                              │ MyBatis 공간 쿼리
[API Gateway (:8100)] ───(/map/**)───> [mapservice-rest]
                                              │
                                              ▼ GeoJSON 스트리밍
[웹 프론트엔드 (sj-lab-mapservice)] <────────┘
```

### 3.2 핵심 업무 프로세스

1. **공간 레이어 조회 프로세스**:
   - 클라이언트 뷰포트의 BBOX(`minX, minY, maxX, maxY`, EPSG:3857)와 레이어 종류, 행정구역 코드를 전달받음.
   - 공간 인덱스(GIST)를 활용하여 영역 내 객체를 조회하고, 설정된 limit을 초과하는 경우 화면 전체에 균등 분산되도록 그리드 파티셔닝 샘플링 적용.
   - PostGIS 엔진 내부에서 `json_build_object` 및 `ST_AsGeoJSON`을 통해 완성된 GeoJSON 문자열을 생성하여 애플리케이션 직렬화 비용 없이 반환.
2. **시설물 내업(Office Work) 처리 프로세스**:
   - 보수 필요(`repair_required_yn='Y'`) 판정 시설물에 대해 웹에서 접수(`RECEIVED`), 진행(`IN_PROGRESS`), 완료(`DONE`), 보류(`HOLD`) 상태를 변경.
   - 완료 상태 변경 시 완료일자(`complete_date`) 검증 필수 적용.
   - 내업 테이블 미생성 또는 마이그레이션 지연 환경에서도 기본 시설물 표출이 중단되지 않도록 Graceful Degradation(단계적 폴백) 적용.
3. **증빙 사진 및 원격 미디어 처리 프로세스**:
   - **사무실 조치 사진**: 내업 ID 단위로 전/후 사진 최대 5장까지 바이너리(`bytea`)로 저장. 파일 시그니처(Magic Number) 검증을 거쳐 JPEG/PNG/WebP만 허용하며 비관적 락(`FOR UPDATE`)으로 정원 초과 방지.
   - **현장 미디어 중계**: QFieldCloud에 저장된 현장 음성/사진/영상을 다운로드 중계하며, DB에 등록된 첨부 경로 여부를 검증한 후 전송.

---

## 4. 핵심 엔지니어링 구현 상세

### 4.1 PostGIS 레벨 GeoJSON 직접 조립 (Zero-Serialization)
대용량 공간 데이터 전송 시 애플리케이션 메모리 부하와 DTO 변환 오버헤드를 방지하기 위해, SQL 엔진 내부에서 GeoJSON 구조를 직접 빌드합니다.

```sql
SELECT json_build_object(
  'type', 'FeatureCollection',
  'features', coalesce(json_agg(
    json_build_object(
      'type', 'Feature',
      'geometry', ST_AsGeoJSON(f.geom)::json,
      'properties', json_build_object(
        'total_id', f.total_id,
        'fclt_nm', f.fclt_nm,
        'facility_condition', f.facility_condition,
        'repair_required_yn', f.repair_required_yn,
        'office_work_status', coalesce(ow.work_status, CASE WHEN f.repair_required_yn = 'Y' THEN 'PENDING' ELSE null END)
      )
    )
  ), '[]'::json)
)::text AS geojson
FROM qfield.facility_total_view f
LEFT JOIN LATERAL (
  SELECT work_status FROM map.facility_office_work
  WHERE total_id = f.total_id ORDER BY work_id DESC LIMIT 1
) ow ON true
```

### 4.2 대용량 WFS 레이어의 BBOX 격자 표본화 (Spatial Grid Sampling)
전국 단위 수십만 건의 공간 객체(버스정류장 85MB, 병원 61MB 등)를 초기 로딩 시 모두 전송하면 브라우저 렌더링 지연이 발생합니다.
- 단순 `LIMIT` 사용 시 특정 지역에 데이터가 쏠리는 현상을 방지하기 위해 BBOX를 가상 격자로 분할.
- `row_number() OVER (PARTITION BY grid_x, grid_y)` 윈도우 함수를 사용하여 화면 전 영역에 균일하게 분포된 대표 객체를 선별 반환.
- 응답 데이터는 gzip 압축(1/10 수준 축소) 및 `Cache-Control`을 차등 적용하여 네트워크 전송 효율 극대화.

### 4.3 공간 인덱스(GIST) 친화적 조인 및 행정구역 필터
- EPSG:3857 점 좌표와 EPSG:4326 행정구역 폴리곤을 교차 검사할 때, 무거운 폴리곤을 변환하지 않고 점 좌표만 `ST_Transform(geom, 4326)`으로 변환하여 `g_emd`의 GIST 인덱스를 100% 활용합니다.
- 행정구역 계층 코드(시도 2자리, 시군구 5자리, 읍면동 8자리)의 접두사 일치 규칙을 이용해 `emd_cd LIKE :code || '%'` 인덱스 레인지 스캔을 수행합니다.

### 4.4 결함 허용(Graceful Degradation) 및 동적 스키마 감지
- 내업 테이블(`map.facility_office_work`)이 배포 시점에 존재하지 않더라도 서비스 전체가 장애에 빠지지 않도록 `to_regclass` 시스템 카탈로그 조회 결과를 캐싱합니다.
- 테이블 부재 시 자동으로 폴백 쿼리를 실행하여 지도 조회를 정상 지원하며, 60초 주기 재확인 또는 DDL 실행 후 무중단 자동 감지를 지원합니다.

---

## 5. 주요 API 명세

| 메서드 | 경로 | 설명 | 파라미터 / 특징 |
|---|---|---|---|
| `GET` | `/map/check` | 게이트웨이 라우팅 및 백엔드 헬스체크 | 인스턴스 포트 메시지 반환 (게이트웨이 연결 확인용) |
| `GET` | `/map/welcome` | 서비스 연결 웰컴 엔드포인트 | 웰컴 문자열 반환 |
| `GET` | `/map/wfs/{layerName}` | 공공데이터 공간 레이어 조회 | `bbox`, `limit` (편의점, 버스정류장, CCTV, 병원 등) |
| `GET` | `/map/qfield/facilities` | QField 점검 시설물 목록 조회 | `bbox`, `limit`, `admCd`, `condition`, `repairYn` |
| `GET` | `/map/admin-area/{sido\|sgg\|emd}` | 행정구역 폴리곤 및 중심점 조회 | `admCd` 계층 필터링 |
| `GET` | `/map/admin-area/bbox` | 행정구역 경계 BBOX 반환 | `admCd` 기준 화면 이동 영역 계산 |
| `GET` | `/map/qfield/office-works/{totalId}` | 특정 시설물의 내업 이력 조회 | 내업 단계별 처리 상세 및 완료일자 |
| `POST` | `/map/qfield/office-works` | 신규 내업 등록 및 상태 갱신 | `totalId`, `workStatus`, `content`, `completeDate` |
| `POST` | `/map/qfield/office-works/{workId}/photos` | 내업 전/후 사진 업로드 | `multipart/form-data`, 사진 최대 5장, 바이너리 검증 |
| `GET` | `/map/qfield/facilities/{id}/media` | QFieldCloud 원격 첨부파일 중계 | 토큰 위임 인증, MIME 자동 보정 스트리밍 |

---

## 6. 실행 및 환경 구성

### 로컬 개발 환경 기동
```powershell
# Maven 빌드
mvnw.cmd clean package

# 로컬 프로파일 실행 (Eureka: localhost:8761 등록)
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

### 필수 환경 변수
- `QFIELD_USERNAME`: QFieldCloud 연동 계정 ID (미디어 중계 시 필수)
- `QFIELD_PASSWORD`: QFieldCloud 연동 계정 비밀번호
- `QFIELD_BASE_URL`: 기본값 `https://qfield.sj-lab.co.kr`
