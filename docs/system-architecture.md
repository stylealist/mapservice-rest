# sj-lab 시스템 전체 구조 (DB → 프론트엔드)

이 저장소(`mapservice-rest`)에서 띄운 총괄 세션이 DB부터 프론트엔드까지 한 번에 보고 작업하기 위한 지도입니다. 로컬 경로·포트·CORS는 `docs/dev-environment.md`, MCP·DB 접속은 `docs/mcp.md`를 봅니다. 각 저장소 코드 규칙의 원본은 그 저장소의 `CLAUDE.md`이며, 여기에는 저장소를 넘나들 때 필요한 사실만 요약합니다.

## 계층 구성

```
[브라우저] sj-lab-mapservice (정적 SPA, OpenLayers)
     │  getApiUrl("/map/...")  로컬 http://localhost:8100 / 운영 https://api.sj-lab.co.kr
     ▼
[게이트웨이] sj-lab-apigateway :8100 (Spring Cloud Gateway, CORS)
     │  /map/**        → lb://MAPSERVICE-REST
     │  /scheduler/**  → lb://SJ-LAB-SCHEDULER
     │  /fast-api-ai/** → lb://FAST-API-AI        (모두 Eureka에서 인스턴스 조회)
     ▼                                              ▲ 등록/조회
[백엔드] mapservice-rest   (조회, context-path /map) ─┐
[배치]   sj-lab-scheduler  (수집, context-path /scheduler) ─┼─ [디스커버리] sj-lab-discoveryServer :8761 (Eureka)
[AI]     fast-api-ai       (FastAPI :8000, root_path /fast-api-ai) ─┘
     │ 읽기: mapservice-rest (MyBatis, GeoJSON을 DB에서 조립)
     │ 쓰기: sj-lab-scheduler (공공 API 수집 → INSERT + CREATE OR REPLACE VIEW map.v_*_geojson)
     ▼
[DB] PostgreSQL 17 + PostGIS 3.4 (sjlab)            ← [외부] 공공데이터포털 · ITS · 생활안전지도
  ▲ qfield 스키마 적재
[동기화] sj-qfieldsync (파이썬 워커, 30초 주기) ← QFieldCloud ← [현장조사 앱] infra-manage-app (QField 포크)
```

**데이터가 지도에 뜨기까지**: scheduler cron이 외부 API 수집 → `map.*` 테이블 적재 → `map.v_*_geojson` 뷰 재생성 → mapservice-rest가 뷰의 `geojson` 컬럼 select → 게이트웨이 → 프론트 `map-wfs.js`. 즉 WFS 레이어의 **뷰 정의 원본은 DB가 아니라 scheduler의 매퍼 XML**입니다.

| 계층 | 저장소 · 로컬 경로 | 기술 스택 | Eureka 이름 | 코드 규칙 |
|---|---|---|---|---|
| 첫 화면 | `sj-lab-hub` · `C:\vscode_develop\sj-lab-hub` | React 18 + Webpack, 라우터 없이 `window.location.href`로 이동 | - | 그 저장소 `CLAUDE.md` |
| 지도 프론트엔드 | `sj-lab-mapservice` · `C:\vscode_develop\sj-lab-mapservice` | 순수 정적 JS(ES 모듈), OpenLayers·hls.js 벤더링, 빌드 도구 없음 | - | 그 저장소 `CLAUDE.md` + `docs/map-architecture.md`, `ui-conventions.md`, `external-services.md` |
| 게이트웨이 | `sj-lab-apigateway` · `C:\developer\workspace\sj-lab-apigateway` | Spring Boot 3.3.2, Spring Cloud 2023.0.3, Gateway(WebFlux) | `apigateway-service` | 그 저장소 `CLAUDE.md` |
| 디스커버리 | `sj-lab-discoveryServer` · `C:\developer\workspace\sj-lab-discoveryServer` | Spring Boot 3.3.2, Eureka Server | `discoveryservice` (자기 등록 안 함) | 그 저장소 `CLAUDE.md` |
| 백엔드 | `mapservice-rest` · `C:\developer\workspace\mapservice-rest` | Spring Boot 3.3.2, MyBatis, PostgreSQL 드라이버 | `MAPSERVICE-REST` (`spring.application.name: mapservice-rest`) | 이 저장소 `CLAUDE.md` |
| 배치 | `sj-lab-scheduler` · `C:\developer\workspace\sj-lab-scheduler` | Spring Boot 3.3.2, MyBatis, `@Scheduled` | `SJ-LAB-SCHEDULER` | 그 저장소 `CLAUDE.md`(도메인별 cron 표 포함) |
| AI | `fast-api-ai` · `C:\developer\workspace\fast-api-ai` | Python 3.12, FastAPI, py-eureka-client | `FAST-API-AI` (`APP_NAME: fast-api-ai`) | 그 저장소 `CLAUDE.md` |
| DB | 개발 DB `sjlab` (MCP `sjlabDevDb`, 읽기 전용) | PostgreSQL 17.0 + PostGIS 3.4.3 | - | `docs/analysis/*.md` |
| 배포 | `sj-lab-k8s-manifests` · `C:\developer\workspace\sj-lab-k8s-manifests` | 서비스별 Helm 차트, ArgoCD GitOps 소스 | - | 그 저장소 `CLAUDE.md` |
| 현장조사 앱 | `infra-manage-app` · `C:\vscode_develop\infra-manage-app` | QField 포크(C++/QML, CMake+vcpkg). 기본 브랜치 `master` | - | 그 저장소 `CLAUDE.md` |
| 수집 동기화 | `sj-qfieldsync` · `C:\vscode_develop\sj-qfieldsync` | 파이썬 단일 워커(30초 주기), QFieldCloud → PostGIS | - | 그 저장소 `CLAUDE.md` |

**뷰와 생성 주체 (scheduler 매퍼 XML 기준)**: `map.v_convenience_store_geojson`, `v_bus_stop_info_geojson`, `v_cctv_info_geojson`, `v_pharmacy_info_geojson`, `v_hospital_info_geojson`, `v_government_office_geojson`, `v_fclt_info`, `v_fclt_info_geojson`. `qfield.facility_total_view`와 `public.g_sido/g_sgg/g_emd`는 scheduler가 만들지 않습니다.

**시설물 데이터의 출처 (QField 계열)**: 현장조사 앱 `infra-manage-app`(QField 포크)으로 입력한 데이터가 QFieldCloud(**https://qfield.sj-lab.co.kr**)에 올라가고, `sj-qfieldsync` 워커가 30초 주기로 변경된 프로젝트만 감지해 GPKG를 내려받아 PostGIS `qfield` 스키마로 적재합니다. 프로젝트 테이블이 추가·삭제되면 같은 워커가 **`qfield.facility_total_view`(통합 뷰)를 재생성**합니다 — 즉 이 뷰의 정의 주체는 `sj-qfieldsync`입니다. 시설물 컬럼이 바뀌면 그 저장소부터 확인하세요.

**설정 테이블 `qfield.facility_icon`**: 지도 시설물 아이콘(종류 판별 키워드·라벨·SVG 글리프·색상)을 담습니다. 생성·초기데이터 스크립트는 `db/qfield_facility_icon.sql`이며, **DDL 실행은 에이전트가 하지 않고 DB 권한이 있는 담당자가 직접 합니다**(프로젝트 규칙). 테이블이 없으면 API가 빈 배열을 돌려주고 프론트는 내장 기본 아이콘으로 동작하므로, 스크립트 실행 전에도 지도는 정상입니다. 아이콘을 추가·변경할 때는 프론트 코드가 아니라 이 테이블 행을 고칩니다.

## API 계약 (DB ↔ 백엔드 ↔ 프론트)

게이트웨이는 경로를 벗기지 않고 `/map/...`을 그대로 넘기며, 백엔드 context-path가 `/map`이므로 컨트롤러 매핑은 `/map` 뒤 부분입니다.

| 외부 경로 (게이트웨이) | 백엔드 컨트롤러 | DB 원천 | 프론트 호출 위치 |
|---|---|---|---|
| `GET /map/convenience-store?bbox=&limit=` | `WfsController` | `map.convenience_store`(bbox) / `map.v_convenience_store_geojson`(전체) | `js/modules/map/map-wfs.js` |
| `GET /map/busStop-info?bbox=&limit=` | `WfsController` | `map.bus_stop_info`(bbox) / `map.v_bus_stop_info_geojson`(전체) | `map-wfs.js` |
| `GET /map/cctv-info?bbox=&limit=` | `WfsController` | `map.cctv_info`(bbox) / `map.v_cctv_info_geojson`(전체) | `map-wfs.js` |
| `GET /map/pharmacy-info?bbox=&limit=` | `WfsController` | `map.pharmacy`(bbox) / `map.v_pharmacy_info_geojson`(전체) | `map-wfs.js` |
| `GET /map/hospital-info?bbox=&limit=` | `WfsController` | `map.hospital`(bbox) / `map.v_hospital_info_geojson`(전체) | `map-wfs.js` |
| `GET /map/governmentOffice-info?bbox=&limit=` | `WfsController` | `map.government_office`(bbox) / `map.v_government_office_geojson`(전체) | `map-wfs.js` |
| `GET /map/qfield/facilities?sidoCd=&sggCd=&emdCd=` | `QfieldFacilityController` | `qfield.facility_total_view` + `public.g_emd` | `js/modules/map/map-facility.js` |
| `GET /map/qfield/facilities/{totalId}` | `QfieldFacilityController` | `qfield.facility_total_view`, `public.g_emd/g_sgg/g_sido` | `map-facility.js` |
| `GET /map/qfield/facilities/{totalId}/media?path=` | `QfieldFacilityController` → `QfieldMediaService` | `qfield.facility_total_view` + QFieldCloud 원본 파일 | `map-facility.js` (`buildFacilityMediaUrl`) |
| `GET /map/qfield/facility-icons` | `QfieldFacilityController` | `qfield.facility_icon` | `map-facility.js` (`loadFacilityIconConfig`) |
| `GET /map/admin-area/sido` | `QfieldFacilityController` | `public.g_sido` | `map-facility.js` |
| `GET /map/admin-area/sgg?sidoCd=` | `QfieldFacilityController` | `public.g_sgg` | `map-facility.js` |
| `GET /map/admin-area/emd?sggCd=` | `QfieldFacilityController` | `public.g_emd` | `map-facility.js` |

- **시설물 첨부 파일(사진·음성·영상)**: `photo_1`~`photo_5`, `audio_memo`, `video` 컬럼에는 URL이 아니라 **QField 프로젝트 안의 상대 경로**가 들어 있습니다(예: `DCIM/JPEG_20260916071830596.jpg`, `audio/AUDIO_...m4a`, `video/VIDEO_...mp4`). 원본 파일은 QFieldCloud(**https://qfield.sj-lab.co.kr**)에 있고 **API가 인증을 요구**하며(`/api/v1/` → 401), `sj-qfieldsync`는 처리 후 내려받은 폴더를 삭제하고(`shutil.rmtree`) 차트 볼륨도 `emptyDir`라 파일이 남지 않습니다. 그래서 **백엔드가 대신 받아 전달하는 중계 엔드포인트**(`/map/qfield/facilities/{totalId}/media?path=...`)를 통해 재생합니다 — 프론트는 상대 경로를 이 URL로 조립하기만 합니다.
  - 중계 흐름: `POST /api/v1/auth/login/`(토큰, 6시간 캐시) → `GET /api/v1/projects/`(`source_table`의 접두어로 프로젝트 식별, 캐시) → `GET /api/v1/files/{projectId}/{경로}/`.
  - **요청된 경로가 그 시설물의 첨부인지 DB로 확인한 뒤에만 전달**합니다(아니면 403). 임의 파일 접근 차단용이므로 이 검증을 빼지 마세요.
  - QFieldCloud가 돌려주는 `Content-Type`은 `application.force-download`라 브라우저가 재생하지 못합니다. 백엔드가 **확장자로 실제 타입을 정해** 내려줍니다.
  - 계정은 `QFIELD_USERNAME`/`QFIELD_PASSWORD` 환경변수로만 주입합니다(미설정 시 이 엔드포인트만 503, 나머지 기능은 정상).
- 시설물 목록(`/map/qfield/facilities`)의 `properties`는 `total_id`, `fclt_nm`, `inst_nm`, `daddr`, `facility_condition`, `repair_required_yn`, `emd_cd`입니다. `inst_nm`·`daddr`는 같은 이름(예: "강당")이 반복될 때 목록에서 구분하기 위한 보조 정보이므로 빼지 마세요.
- **WFS 레이어의 화면 영역 조회(`bbox`·`limit`)**: 전국 데이터를 통째로 내려주면 버스정류장 85MB·병원 61MB(합계 약 200MB)라 최초 표출이 수십 초 걸렸습니다. 그래서 두 가지 모드를 둡니다.
  - `bbox=minX,minY,maxX,maxY`(**EPSG:3857**, 지도 뷰와 같은 좌표계)를 주면 그 영역 안의 피처만 조립합니다. `limit`은 개수 상한(기본 3000, 최대 20000)이며, 상한을 넘으면 bbox 를 `limit`개 격자로 나눠 **칸마다 하나씩 뽑는 방식으로 화면 전체에 고르게 퍼진 표본**을 내려줍니다(한쪽에 몰리지 않음). bbox 형식이 틀리면 400.
  - `bbox` 없이 부르면 **기존대로 전국 전체**를 내려줍니다(뷰 그대로). 이전 버전 프론트가 붙어도 동작하게 하려고 남겨 둔 경로입니다.
  - bbox 모드의 SQL은 뷰가 아니라 원본 테이블(`map.bus_stop_info` 등)을 직접 조회합니다. **뷰와 같은 중복 제거(`distinct on` 좌표)와 같은 properties 구성을 `wfs-geojson.xml`에 옮겨 적어 둔 것이므로, scheduler 쪽 뷰 정의가 바뀌면 이 XML도 같은 작업에서 함께 고쳐야 합니다.**
  - 프론트는 화면보다 가로·세로 50% 넓은 영역을 받아 두고, 그 안에서 움직이는 동안은 요청하지 않습니다(`map-wfs.js`의 `wfsFetchState`).
- **응답 압축**: `server.compression`이 켜져 있어 GeoJSON 응답은 gzip으로 나갑니다(실측 10배 이상 축소). 게이트웨이는 `Accept-Encoding`을 그대로 넘기므로 브라우저까지 적용됩니다.
- 응답 형식: WFS 레이어는 `geojson` 텍스트(FeatureCollection), 오류 시 **HTTP 200 + 빈 바디**. QField/행정구역은 JSON 문자열 + 400/404 명시, `Cache-Control` 60초(시설물)·3600초(행정구역), WFS 300초.
- 좌표계: 시설물 `geom`은 EPSG:3857, 행정구역 경계는 EPSG:4326. 프론트 지도 뷰는 EPSG:3857입니다.
- 행정구역 코드는 접두어 계층(sido 2자리 → sgg 5자리 → emd 8자리)이며 백엔드와 프론트가 같은 자릿수 검증을 가정합니다.
- 이 표는 코드에서 확인한 사실만 담습니다. 엔드포인트를 추가·변경하면 이 표도 함께 고칩니다.

## 저장소를 넘나드는 변경 체크리스트

**새 지도 레이어 (DB → 프론트)**
0. 데이터 수집(scheduler): 외부 API에서 새로 가져와야 하면 scheduler의 `add-data-scheduler` 스킬 패턴(그 저장소 `.claude/skills/`)으로 수집·적재 배치를 추가하고, cron 표에서 시간대가 겹치지 않는지 확인.
1. DB 뷰: WFS 패턴이면 scheduler 매퍼 XML에 `CREATE OR REPLACE VIEW map.v_xxx_geojson`(`geojson` 컬럼)을 추가해 적재 직후 재생성되게 함. 뷰를 둘 수 없으면 mapservice-rest XML에서 `json_build_object`/`ST_AsGeoJSON`으로 조립. 개발 DB는 MCP로 읽기 전용 조회만 가능하므로 수동 DDL은 사용자에게 요청.
2. 백엔드: `add-wfs-layer` 스킬 또는 `qfield-facility.xml` 패턴으로 Mapper·XML·Service·Controller 추가.
3. 게이트웨이: `/map/**` 아래 경로면 변경 불필요.
4. 프론트: `map-wfs.js`의 설정 배열에 `getApiUrl("/map/...")`로 추가하고, 인라인 HTML에서 부를 함수는 `map.js`에서 `window.*`에 등록. 레이어 패널 UI는 `docs/ui-conventions.md` 참고.
5. 이 문서의 API 계약 표 갱신.

**새 마이크로서비스 / 경로 prefix**
- 서비스는 Eureka에 등록(`spring.application.name`), 게이트웨이 `application.yml`의 `spring.cloud.gateway.routes`에 `lb://SERVICE-ID` + `Path=/prefix/**` + `CustomFilter`·`PreserveHostHeader` 추가. `FilterConfig.java`(주석 처리된 예시)는 건드리지 않음.

**새 프론트 도메인·포트**
- 게이트웨이 `globalcors.allowedOrigins`에 추가해야 합니다. 추가하지 않으면 브라우저에서 403.

**응답 형식 변경**
- 프론트 `map-wfs.js`/`map-facility.js`의 파싱·스타일 코드를 같은 작업에서 함께 수정합니다. 백엔드만 바꾸면 지도에서 조용히 사라집니다(WFS는 200 + 빈 바디).

**뷰·테이블 컬럼 변경**
- scheduler의 뷰 정의(`CREATE OR REPLACE VIEW`)나 적재 컬럼을 바꾸면 mapservice-rest 매퍼 XML과 프론트 속성 표시(팝업·스타일)까지 같은 작업에서 확인합니다. 뷰는 다음 배치 실행 때 재생성되므로, 배포 직후 기존 뷰와 코드가 어긋날 수 있습니다.

**fast-api-ai 라우트 추가**
- `routes/<도메인>/`에 `APIRouter`를 만들고 `main.py`에서 `include_router()`로 등록해야 노출됩니다(그 저장소 `add-route` 스킬). 게이트웨이 경로는 `/fast-api-ai/...`.

## 배포 경로 (운영)

```
git push → Jenkins(빌드 → 이미지 push: sj-lab-registry.kr.ncr.ntruss.com)
        → sj-lab-k8s-manifests 의 <서비스>/values.yaml 의 image.tag 를 자동 커밋
        → ArgoCD 가 동기화(selfHeal·prune) → 쿠버네티스 롤아웃
```

- **`image.tag`는 Jenkins가 관리하는 값**입니다. 요청 없이 임의로 낮추거나 되돌리지 마세요.
- 매니페스트 단계는 clone → sed → push 구조라 **여러 저장소를 동시에 push하면 한 잡이 `cannot lock ref`로 실패**할 수 있습니다(2026-09-16 실제 발생). 실패하면 이미지는 레지스트리에 올라가 있고 태그 커밋만 빠진 상태이므로, 해당 잡을 재실행하면 됩니다.
- 롤아웃 중에는 게이트웨이가 잠시 **503**을 반환합니다(옛 파드 종료 ~ 새 파드의 Eureka 등록 사이). 배포 직후 503은 몇 초 뒤 다시 확인해 보세요.
- 차트를 고쳤다면 해당 차트 디렉터리에서 `helm lint`와 `helm template`을 돌려 렌더링을 확인합니다. 로컬 저장소가 Jenkins 자동 커밋보다 뒤처져 있을 수 있으니 **수정 전 `git pull`** 하세요.

## 총괄 세션에서 다른 저장소를 다룰 때

- `.claude/settings.local.json`의 `permissions.additionalDirectories`에 다섯 저장소(게이트웨이·디스커버리·scheduler·fast-api-ai·프론트)가 등록되어 있어, 이 세션에서 바로 읽고 수정할 수 있습니다(로컬 전용 설정). 폴더 신뢰 등록 위치는 `docs/dev-environment.md` 참고.
- 다른 저장소의 `CLAUDE.md`는 이 세션에 자동으로 로드되지 않습니다. 그 저장소 파일을 **수정하기 전에 해당 저장소의 `CLAUDE.md`(프론트는 `docs/*.md`까지)를 Read로 먼저 읽을 것.**
- 저장소마다 git이 따로입니다. 상태 확인·커밋은 `git -C <경로> ...`로 저장소별로 하고, 한 작업이 여러 저장소에 걸치면 저장소마다 커밋합니다.
- 다른 저장소의 훅·에이전트는 이 세션에서 동작하지 않으므로 직접 지켜야 합니다.
  - `sj-lab-discoveryServer`: `target/`가 git에 추적되지만 편집 금지(그 저장소 훅이 막던 규칙). 설정은 `src/main/resources/`만 수정.
  - `sj-lab-scheduler`: 커밋 전 staged diff에 `password`/`secret`/`api_key`/`service_key` 등이 **새로 추가**됐는지 확인(그 저장소 `check-secrets.sh` 훅 규칙). 기존에 커밋된 키는 사용자와 상의 없이 로테이션·이전하지 않음. 로컬에서 띄우면 cron 배치가 실제 DB에 적재하므로 검증용 기동은 사용자 확인 후에.
  - `sj-lab-k8s-manifests`: 차트 수정 전 `git pull`(Jenkins 자동 커밋이 계속 쌓임), 수정 후 `helm lint <차트>`·`helm template <차트>` 확인. `image.tag`는 Jenkins 관리 값이므로 임의 변경 금지. 네임스페이스·리소스 제한 등은 같은 차트의 기존 패턴을 따를 것.
  - `sj-qfieldsync`: QFieldCloud 메타 DB(`QFC_DB`)는 읽기 위주, 적재 대상은 PostGIS `qfield` 스키마로 서로 다른 두 DB를 다룹니다. 문법 확인은 `python -m py_compile qfield_data_sync.py`. **로컬에서 워커를 돌리면 실제 DB에 적재되므로 사용자 확인 후에** 실행할 것.
  - `infra-manage-app`: 업스트림 QField 포크라 **커스텀 변경은 최소 지점에 집중**하고 업스트림 구조를 유지할 것. 기본 브랜치가 `master`(다른 저장소는 `main`)이고, CMake+vcpkg 전체 빌드는 수 시간이 걸리므로 빌드 전에 기존 빌드 디렉터리와 대상 플랫폼을 확인할 것.
  - `sj-lab-hub`: 기능 카드는 `src/App.js` 최상단 `features` 배열 하나가 단일 소스. 스타일은 파일 하단의 인라인 `xxxStyle` 객체 컨벤션을 유지하고, 검증은 `npm start`(3000) 또는 `npm run build`로 할 것.
  - `fast-api-ai`: `.py` 수정 후 `python -m py_compile <파일>`로 문법 확인(그 저장소 훅 규칙). `core/config.py`의 `INSTANCE_IP` 고정(127.0.0.1)은 의도된 것이므로 되돌리지 않음.
  - 각 저장소의 리뷰 체크리스트는 `<저장소>/.claude/agents/reviewer.md`에 있습니다. 이 세션의 `reviewer` 에이전트는 `mapservice-rest` 전용이므로, 다른 저장소 변경은 그 체크리스트 파일을 읽고 확인합니다.
- `sj-lab-mapservice`와 `mapservice-rest`는 public 저장소입니다. DB 호스트·비밀번호·토큰을 문서나 코드에 적지 않습니다.

