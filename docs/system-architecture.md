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
```

**데이터가 지도에 뜨기까지**: scheduler cron이 외부 API 수집 → `map.*` 테이블 적재 → `map.v_*_geojson` 뷰 재생성 → mapservice-rest가 뷰의 `geojson` 컬럼 select → 게이트웨이 → 프론트 `map-wfs.js`. 즉 WFS 레이어의 **뷰 정의 원본은 DB가 아니라 scheduler의 매퍼 XML**입니다.

| 계층 | 저장소 · 로컬 경로 | 기술 스택 | Eureka 이름 | 코드 규칙 |
|---|---|---|---|---|
| 프론트엔드 | `sj-lab-mapservice` · `C:\vscode_develop\sj-lab-mapservice` | 순수 정적 JS(ES 모듈), OpenLayers·hls.js 벤더링, 빌드 도구 없음 | - | 그 저장소 `CLAUDE.md` + `docs/map-architecture.md`, `ui-conventions.md`, `external-services.md` |
| 게이트웨이 | `sj-lab-apigateway` · `C:\developer\workspace\sj-lab-apigateway` | Spring Boot 3.3.2, Spring Cloud 2023.0.3, Gateway(WebFlux) | `apigateway-service` | 그 저장소 `CLAUDE.md` |
| 디스커버리 | `sj-lab-discoveryServer` · `C:\developer\workspace\sj-lab-discoveryServer` | Spring Boot 3.3.2, Eureka Server | `discoveryservice` (자기 등록 안 함) | 그 저장소 `CLAUDE.md` |
| 백엔드 | `mapservice-rest` · `C:\developer\workspace\mapservice-rest` | Spring Boot 3.3.2, MyBatis, PostgreSQL 드라이버 | `MAPSERVICE-REST` (`spring.application.name: mapservice-rest`) | 이 저장소 `CLAUDE.md` |
| 배치 | `sj-lab-scheduler` · `C:\developer\workspace\sj-lab-scheduler` | Spring Boot 3.3.2, MyBatis, `@Scheduled` | `SJ-LAB-SCHEDULER` | 그 저장소 `CLAUDE.md`(도메인별 cron 표 포함) |
| AI | `fast-api-ai` · `C:\developer\workspace\fast-api-ai` | Python 3.12, FastAPI, py-eureka-client | `FAST-API-AI` (`APP_NAME: fast-api-ai`) | 그 저장소 `CLAUDE.md` |
| DB | 개발 DB `sjlab` (MCP `sjlabDevDb`, 읽기 전용) | PostgreSQL 17.0 + PostGIS 3.4.3 | - | `docs/analysis/*.md` |

**뷰와 생성 주체 (scheduler 매퍼 XML 기준)**: `map.v_convenience_store_geojson`, `v_bus_stop_info_geojson`, `v_cctv_info_geojson`, `v_pharmacy_info_geojson`, `v_hospital_info_geojson`, `v_government_office_geojson`, `v_fclt_info`, `v_fclt_info_geojson`. `qfield.facility_total_view`와 `public.g_sido/g_sgg/g_emd`는 scheduler가 만들지 않습니다(출처 미확인).

**설정 테이블 `qfield.facility_icon`**: 지도 시설물 아이콘(종류 판별 키워드·라벨·SVG 글리프·색상)을 담습니다. 생성·초기데이터 스크립트는 `db/qfield_facility_icon.sql`이며, **DDL 실행은 에이전트가 하지 않고 DB 권한이 있는 담당자가 직접 합니다**(프로젝트 규칙). 테이블이 없으면 API가 빈 배열을 돌려주고 프론트는 내장 기본 아이콘으로 동작하므로, 스크립트 실행 전에도 지도는 정상입니다. 아이콘을 추가·변경할 때는 프론트 코드가 아니라 이 테이블 행을 고칩니다.

## API 계약 (DB ↔ 백엔드 ↔ 프론트)

게이트웨이는 경로를 벗기지 않고 `/map/...`을 그대로 넘기며, 백엔드 context-path가 `/map`이므로 컨트롤러 매핑은 `/map` 뒤 부분입니다.

| 외부 경로 (게이트웨이) | 백엔드 컨트롤러 | DB 원천 | 프론트 호출 위치 |
|---|---|---|---|
| `GET /map/convenience-store` | `WfsController` | `map.v_convenience_store_geojson` | `js/modules/map/map-wfs.js` |
| `GET /map/busStop-info` | `WfsController` | `map.v_bus_stop_info_geojson` | `map-wfs.js` |
| `GET /map/cctv-info` | `WfsController` | `map.v_cctv_info_geojson` | `map-wfs.js` |
| `GET /map/pharmacy-info` | `WfsController` | `map.v_pharmacy_info_geojson` | `map-wfs.js` |
| `GET /map/hospital-info` | `WfsController` | `map.v_hospital_info_geojson` | `map-wfs.js` |
| `GET /map/governmentOffice-info` | `WfsController` | `map.v_government_office_geojson` | `map-wfs.js` |
| `GET /map/qfield/facilities?sidoCd=&sggCd=&emdCd=` | `QfieldFacilityController` | `qfield.facility_total_view` + `public.g_emd` | `js/modules/map/map-facility.js` |
| `GET /map/qfield/facilities/{totalId}` | `QfieldFacilityController` | `qfield.facility_total_view`, `public.g_emd/g_sgg/g_sido` | `map-facility.js` |
| `GET /map/qfield/facility-icons` | `QfieldFacilityController` | `qfield.facility_icon` | `map-facility.js` (`loadFacilityIconConfig`) |
| `GET /map/admin-area/sido` | `QfieldFacilityController` | `public.g_sido` | `map-facility.js` |
| `GET /map/admin-area/sgg?sidoCd=` | `QfieldFacilityController` | `public.g_sgg` | `map-facility.js` |
| `GET /map/admin-area/emd?sggCd=` | `QfieldFacilityController` | `public.g_emd` | `map-facility.js` |

- 응답 형식: WFS 레이어는 `geojson` 텍스트(FeatureCollection), 오류 시 **HTTP 200 + 빈 바디**. QField/행정구역은 JSON 문자열 + 400/404 명시, `Cache-Control` 60초(시설물)·3600초(행정구역).
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

## 총괄 세션에서 다른 저장소를 다룰 때

- `.claude/settings.local.json`의 `permissions.additionalDirectories`에 다섯 저장소(게이트웨이·디스커버리·scheduler·fast-api-ai·프론트)가 등록되어 있어, 이 세션에서 바로 읽고 수정할 수 있습니다(로컬 전용 설정). 폴더 신뢰 등록 위치는 `docs/dev-environment.md` 참고.
- 다른 저장소의 `CLAUDE.md`는 이 세션에 자동으로 로드되지 않습니다. 그 저장소 파일을 **수정하기 전에 해당 저장소의 `CLAUDE.md`(프론트는 `docs/*.md`까지)를 Read로 먼저 읽을 것.**
- 저장소마다 git이 따로입니다. 상태 확인·커밋은 `git -C <경로> ...`로 저장소별로 하고, 한 작업이 여러 저장소에 걸치면 저장소마다 커밋합니다.
- 다른 저장소의 훅·에이전트는 이 세션에서 동작하지 않으므로 직접 지켜야 합니다.
  - `sj-lab-discoveryServer`: `target/`가 git에 추적되지만 편집 금지(그 저장소 훅이 막던 규칙). 설정은 `src/main/resources/`만 수정.
  - `sj-lab-scheduler`: 커밋 전 staged diff에 `password`/`secret`/`api_key`/`service_key` 등이 **새로 추가**됐는지 확인(그 저장소 `check-secrets.sh` 훅 규칙). 기존에 커밋된 키는 사용자와 상의 없이 로테이션·이전하지 않음. 로컬에서 띄우면 cron 배치가 실제 DB에 적재하므로 검증용 기동은 사용자 확인 후에.
  - `fast-api-ai`: `.py` 수정 후 `python -m py_compile <파일>`로 문법 확인(그 저장소 훅 규칙). `core/config.py`의 `INSTANCE_IP` 고정(127.0.0.1)은 의도된 것이므로 되돌리지 않음.
  - 각 저장소의 리뷰 체크리스트는 `<저장소>/.claude/agents/reviewer.md`에 있습니다. 이 세션의 `reviewer` 에이전트는 `mapservice-rest` 전용이므로, 다른 저장소 변경은 그 체크리스트 파일을 읽고 확인합니다.
- `sj-lab-mapservice`와 `mapservice-rest`는 public 저장소입니다. DB 호스트·비밀번호·토큰을 문서나 코드에 적지 않습니다.
