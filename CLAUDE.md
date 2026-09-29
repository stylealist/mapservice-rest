# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 총괄 저장소는 여기가 아닙니다 (2026-09-30 이전)

저장소를 넘나드는 작업(DB → 백엔드 → 디스커버리 → 게이트웨이 → 프론트엔드 → 배포)의 **총괄 기준 저장소는
`C:\developer\workspace\sj-lab`** 입니다. 총괄 Claude 세션은 그 저장소에서 띄웁니다.

| 찾는 것 | 위치 |
|---|---|
| 시스템 전체 구조·API 계약·변경 체크리스트 | `sj-lab/docs/system-architecture.md` |
| 로컬 경로·포트·기동 순서·CORS | `sj-lab/docs/dev-environment.md` |
| MCP 설정·비밀값 관리 | `sj-lab/docs/mcp.md`, `sj-lab/.mcp.json`, `sj-lab/.claude/settings.local.json` |
| 운영 Secret·Jenkins Credential | `sj-lab/docs/k8s-secrets.md` |
| 정적 사이트 배포·Jenkins 파이프라인 | `sj-lab/docs/deploy-static-sites.md`, `sj-lab/docs/jenkins/*.groovy` |
| 로컬 전체 기동 스크립트 | `sj-lab/scripts/local-stack.ps1` (백엔드는 이 저장소에서 빌드한다) |
| 작업 로그(history)·공유 페이지 | `sj-lab/history/` |

이 문서에는 **이 저장소(백엔드) 코드 규칙만** 남깁니다. 공통 규칙(한글 답변, 커밋 전 확인, history 작성,
DB 조회 전용, 파일 삭제 금지 등)은 총괄 저장소의 `CLAUDE.md`를 따릅니다.

## 명령어

Windows: `mvnw.cmd` 사용; Bash 등 Unix 계열 셸에서는 `./mvnw` 사용.

```
mvnw.cmd clean package        # 빌드 (target/sj-lab-mapservice-rest.jar 생성)
mvnw.cmd spring-boot:run       # 로컬 실행 (local 프로파일은 -Dspring-boot.run.profiles=local 추가)
mvnw.cmd test                  # 전체 테스트 실행
mvnw.cmd test -Dtest=MapServiceApplicationTests   # 단일 테스트 클래스 실행
```

이 저장소에는 별도의 lint/포맷터 설정이 없습니다.

Docker: `Dockerfile`은 미리 빌드된 `target/sj-lab-mapservice-rest.jar`가 존재해야 하므로(먼저 `mvnw.cmd clean package` 실행), `ENTRYPOINT ["java", "-jar", ...]`로 실행합니다. 활성 Spring 프로파일은 이미지에 고정하지 않고 외부(Helm)에서 주입합니다.

## 아키텍처

Eureka에 등록되는(`@EnableDiscoveryClient`) Spring Boot 3.3.2 / Java 17 마이크로서비스(`mapservice-rest`)로, 읽기 전용 지도/GIS 데이터를 GeoJSON으로 제공합니다. API 게이트웨이 뒤에 위치하며 context-path는 `/map`, prod 기준 base path는 `/api/map`입니다(`NewSwaggerConfig` 참고).

**요청 흐름**:
- 기존 WFS 레이어: `WfsController`(REST 엔드포인트) → `WfsService` / `WfsServiceImpl` → `WfsMapper`(자바 쪽 SQL이 없는 MyBatis `@Mapper` 인터페이스) → `src/main/resources/mapper/wfs-geojson.xml` → PostgreSQL/PostGIS.
- QField 시설물 및 행정구역 레이어: `QfieldFacilityController` → `QfieldFacilityService` / `QfieldFacilityServiceImpl` → `QfieldFacilityMapper` → `src/main/resources/mapper/qfield-facility.xml` → PostgreSQL/PostGIS.
- 시설물 내업(사무실 처리) 기록: `QfieldOfficeWorkController` → `QfieldOfficeWorkService` / `QfieldOfficeWorkServiceImpl` → `QfieldOfficeWorkMapper` → `src/main/resources/mapper/qfield-office-work.xml` → `map.facility_office_work`. `INSERT/UPDATE ... RETURNING`을 CTE로 감싼 `<select>`가 JSON 한 건을 돌려줍니다. 입력 검증은 서비스가 하고 실패 시 `ValidationException` → 400.
- 내업 처리 전·후 사진: `QfieldOfficeWorkPhotoController` → `QfieldOfficeWorkPhotoService` / `QfieldOfficeWorkPhotoServiceImpl` → `QfieldOfficeWorkPhotoMapper` → `src/main/resources/mapper/qfield-office-work-photo.xml` → `map.facility_office_work_photo`(바이너리 `bytea`). 내업 기록과 함께 이 저장소의 **쓰기** API입니다.

**공개 API와의 관계**: 외부에 여는 창구는 `sj-lab-openapi`(게이트웨이 `/open-api/**`)이며, 그 서비스가 이 저장소의
`/map/**` 엔드포인트를 호출해 중계합니다. 공개 범위는 그 저장소의 `catalog/api-catalog.json`이 정하므로,
여기서 경로·응답 형식을 바꾸면 그 파일의 `upstream` 값도 같은 작업에서 확인할 것.

핵심 포인트:
- 기존 레이어(편의점, 버스정류장, CCTV, 약국, 병원, 관공서): DB 뷰(예: `map.v_bus_stop_info_geojson`)에서 이미 완성된 `geojson` 텍스트 컬럼을 그대로 select할 뿐이며, GeoJSON 조립은 DB에서 이루어집니다. 새 레이어 추가 시 DB 뷰를 생성할 수 있다면 `geojson` 컬럼을 가진 뷰를 추가한 뒤 동일 패턴으로 4개 계층(`WfsMapper`, `wfs-geojson.xml`, `WfsService`/`WfsServiceImpl`, `WfsController`)에 메서드를 추가합니다.
- WFS 레이어의 화면 영역 조회: `WfsController`의 6개 엔드포인트는 `bbox`(EPSG:3857 `minX,minY,maxX,maxY`)와 `limit`을 선택 파라미터로 받습니다. `bbox`가 있으면 뷰가 아니라 **원본 테이블**을 조회하는 `*ByBbox` 문(`wfs-geojson.xml`)을 타고, 없으면 기존처럼 뷰의 전국 데이터를 그대로 내려줍니다(구버전 프론트 호환용). `*ByBbox` 문은 뷰와 같은 중복 제거·properties 구성을 옮겨 적은 것이므로 **scheduler의 뷰 정의가 바뀌면 이 XML도 함께 고쳐야** 결과가 어긋나지 않습니다. 상한을 넘을 때는 bbox 를 격자로 나눠 칸마다 하나씩 뽑아(`row_number`) 화면 전체에 고르게 퍼진 표본을 내려줍니다 — 이 정렬을 단순 `limit`으로 바꾸면 화면 한쪽만 채워집니다.
- 읽기 전용 DB 등 뷰 생성이 불가능한 경우: `qfield-facility.xml` 패턴을 따릅니다. XML 내 SQL에서 `json_build_object`, `json_agg`, `to_jsonb`, `ST_AsGeoJSON` 등을 활용해 DB 레벨에서 GeoJSON/JSON 문자열로 직접 조립해 반환합니다. 파라미터는 반드시 MyBatis `#{}` 바인딩을 사용합니다.
- 시설물 공간 쿼리 및 행정구역 필터: `qfield.facility_total_view`의 EPSG:3857 점 좌표와 `public.g_emd`의 EPSG:4326 경계를 조인할 때, `LEFT JOIN LATERAL`과 `ST_Intersects(e.geom, ST_Transform(f.geom, 4326))` (LIMIT 1)로 `g_emd`의 GIST 인덱스를 활용합니다. 행정구역 코드는 접두어 계층 구조(sido 2자리, sgg 5자리, emd 8자리)이므로 `emd_cd LIKE code || '%'` 단일 조건으로 고속 필터링합니다 (g_sido 폴리곤 직접 조인이나 ST_MakeValid는 지양).
- 행정구역 BBOX 조회: 폴리곤 전체 좌표를 변환하지 않고 `ST_Transform(ST_Envelope(geom), 3857)`로 BBOX만 변환하여 `[minX, minY, maxX, maxY]`를 계산합니다.
- 첨부 파일 중계: 시설물 사진·음성·영상은 DB에 QField 프로젝트 내 상대 경로만 있고 원본은 인증이 필요한 QFieldCloud에 있습니다. `QfieldMediaService`가 토큰 로그인 → 프로젝트 식별 → 파일 다운로드를 거쳐 전달하며, **요청 경로가 그 시설물의 첨부인지 DB로 검증한 뒤에만** 응답합니다(아니면 403). 계정은 `QFIELD_USERNAME`/`QFIELD_PASSWORD` 환경변수로만 주입하고 `application.yml`에 적지 마세요. 로컬은 `sj-lab/scripts/local-stack.ps1`이 `sj-lab/.claude/settings.local.json`의 `env`에서 읽어 넣고, 운영은 `qfield-credentials` Secret에서 받습니다. 값이 없으면 **이 엔드포인트만 503**이 되므로, 첨부만 안 나온다면 계정 주입부터 확인하세요.
- 설정 테이블: 지도 시설물 아이콘은 `map.facility_icon`에서 관리하며 `GET /map/qfield/facility-icons`(`getFacilityIcons`)로 내려줍니다. 생성 스크립트는 `sj-lab/db/map/map_facility_icon.sql`이고 **DDL 실행은 담당자가 직접** 합니다. 테이블이 없거나 조회가 실패하면 서비스가 예외를 삼키고 `null`을 반환해 컨트롤러가 빈 배열(`[]`)을 내려주며, 프론트는 내장 기본 아이콘으로 동작합니다 — 이 경로는 의도된 것이므로 예외를 다시 던지도록 바꾸지 마세요.
- 내업 기록 테이블: `map.facility_office_work`(생성 스크립트 `sj-lab/db/map/map_facility_office_work.sql`)의 `total_id`는 뷰 `qfield.facility_total_view`를 가리키는 **논리적 FK**라 DB 제약이 없습니다. 존재 검증은 서비스(`getFacilityRepairYn`)가 하므로 빼지 마세요. 내업 기록은 웹사이트(내업 작성 화면 → API)로만 쌓이고, **보수 필요(`repair_required_yn='Y'`) 시설물에만 작성**할 수 있습니다(아니면 400). 시설물 목록은 이 테이블을 `LEFT JOIN LATERAL`로 붙여 `office_work_status`를 내려주는데, 보수 필요 시설물이면 최신 기록의 `work_status`(`RECEIVED`/`IN_PROGRESS`/`DONE`/`HOLD`), 기록이 없으면 `PENDING`(미완료), 보수 필요가 아니면 `null`입니다(`office_work_complete_date`도 보수 필요일 때만). 테이블이 없으면 같은 규칙에서 "기록 없음"으로 채운 폴백 쿼리(`getFacilitiesWithoutOfficeWork`)를 탑니다.
- **테이블 스키마 주의**: 앱이 쓰는 테이블(`facility_office_work`, `facility_icon`)은 반드시 `map` 스키마에 둘 것. `qfield` 스키마는 `sj-qfieldsync`가 관리하며, QField 프로젝트 이름 패턴이 아닌 테이블을 "삭제된 프로젝트 테이블"로 보고 `facility_deleted_archive`로 옮긴 뒤 DROP 합니다(2026-09-16 `qfield.facility_icon`, 2026-09-18 `qfield.facility_office_work`가 실제로 삭제됨). 존재 여부는 `QfieldFacilityServiceImpl`이 `to_regclass`로 확인해 캐시하고(없으면 60초마다 재확인, 있다고 캐시한 뒤 조회가 `42P01`로 실패하면 즉시 폴백), 상태가 바뀔 때만 로그 한 줄을 남깁니다. `getFacilities`의 조건·properties를 바꾸면 폴백 select도 같이 고치세요. 첫 쿼리 실패 뒤 폴백이 같은 트랜잭션에 묶여 abort 되지 않도록 `getFacilities`는 `Propagation.NOT_SUPPORTED`입니다.
- **배포 순서 경고(내업)**: 내업 기능을 배포하기 전에 대상 DB에 `sj-lab/db/map/map_facility_office_work.sql`을 먼저 실행할 것. 실행하지 않아도 시설물 목록은 폴백으로 동작하지만(보수 필요 시설물은 `PENDING`, 그 외 `null`) 내업 기록은 저장·조회되지 않습니다(내업 API는 500).
- 내업 사진 업로드(`sj-lab/db/map/map_facility_office_work_photo.sql`): 구분(`BEFORE`/`AFTER`)별 최대 5장(409), 장당 10MB(413 — `application.yml`의 `spring.servlet.multipart.max-file-size`와 `QfieldOfficeWorkPhotoService.maxFileSize`를 같이 바꿀 것), JPEG·PNG·WebP만(400). **`Content-Type`·확장자 외에 파일 시그니처로 실제 이미지인지 확인**하므로 이 검사를 빼지 마세요. 장수 한도는 내업 기록 행을 `FOR UPDATE`로 잠근 뒤 세므로 동시 업로드에도 지켜집니다. multipart 한도 초과는 컨트롤러 전에 발생하므로 `CustomizedResponseEntityExceptionHandler#handleMaxUploadSizeExceededException`이 413 JSON을 돌려주고, 한도 초과 본문도 끝까지 읽도록 `server.tomcat.max-swallow-size`를 20MB로 둡니다(기본 2MB면 연결이 끊겨 413이 전달되지 않을 수 있음). 이 테이블도 `map` 스키마(위 주의)이며 배포 전에 스크립트를 먼저 실행해야 합니다(없으면 사진 API만 500).
- 내업 검증 규칙: `work_status`가 `DONE`이면 `complete_date` 필수(400). 프론트와 같은 규칙이므로 한쪽만 바꾸지 마세요.
- 예외 및 검증: `QfieldFacilityController`는 파라미터 유효성 검증 실패 시 HTTP 400, 시설물 미존재 시 HTTP 404를 명확히 반환하며, 500 오류를 삼키지 않고 정확한 응답 코드를 제공합니다.

매퍼 XML은 `application.yml`의 `mybatis.mapper-locations: classpath*:mapper/**/*.xml` 설정으로 자동 스캔되며, `MapServiceRestApplication`에 `@MapperScan("com.example.mapservice.mapper")`가 선언되어 있습니다.

**설정 계층**: `application.yml`이 기본/운영에 가까운 설정(서버 포트, context-path, datasource, mybatis, actuator)을 담당합니다. `application-local.yml`은 Eureka `defaultZone`을 `localhost:8761`로 덮어쓰고, 로컬 환경에는 게이트웨이가 prefix를 벗겨주지 않으므로 Swagger UI URL에서 `/map` prefix를 제거하도록 재정의합니다.

**보안**: `SecurityConfig`는 CSRF를 비활성화하고 인메모리 사용자 1명만 정의하며, 현재 WFS 엔드포인트에 대한 접근 제어는 없습니다. `MapServiceRestController`(`/welcome`, `/message`, `/check`)와 `HelloWorldController`(`/hello-world*`)는 지도 기능과 무관한 잔존 보일러플레이트/데모 엔드포인트이므로 핵심 로직으로 취급하지 마세요.

**i18n**: `messages*.properties` + `MessageSource`는 `HelloWorldController#helloWorldInternationalized`에서만 사용되며, 다른 곳에는 쓰이지 않습니다.

**예외 처리**: `CustomizedResponseEntityExceptionHandler`가 전역 `@ControllerAdvice`로 존재하지만, `WfsController`의 각 메서드는 예외를 전파하지 않고 자체적으로 catch해서 `e.printStackTrace()`만 호출하고 빈 문자열을 반환합니다. 즉 WFS 엔드포인트가 500이 아니라 HTTP 200 + 빈 바디를 반환할 수 있으니 디버깅 시 유의하세요.

## 변경 시 참고사항

- `application.yml`에 운영 DB 비밀번호가 평문으로 커밋되어 있습니다 — 여기에 추가로 민감정보를 넣지 말고, datasource 설정을 건드릴 경우 자격 증명을 외부화하는 방향을 우선 고려하세요.
- `bean/AdminUser.java`, `bean/AdminUserV2.java`와 `NewSwaggerConfig`의 OpenAPI `/users/**`, `/admin/**` 그룹은 대응하는 컨트롤러가 없는 스캐폴딩입니다 — 미완성/참고용 코드로 보이며 실제 앱에는 연결되어 있지 않습니다.
- `.claude/agents/reviewer.md`(이 저장소 전용 리뷰 체크리스트)와 `.claude/skills/add-wfs-layer`는 여기에 남아 있습니다.

## README 유지 규칙

- **이 저장소에 기능·API·화면·실행 방법·설정(환경변수/시크릿)·배포 방식이 추가되거나 바뀌면, 같은 작업에서 `README.md`도 함께 갱신할 것.** 코드만 고치고 README를 그대로 두지 말 것.
- 갱신 대상 예: 새 엔드포인트·화면·모듈, 빌드/실행 명령 변경, 포트·의존 서비스 변경, 환경변수·Secret 추가, 배포 절차 변경, 해결한 이슈·새로 생긴 한계.
- **README는 면접관·처음 보는 사람이 읽는 문서**다(이 프로젝트는 포트폴리오). 사용자·리뷰어 관점의 설명(무엇을·왜·어떻게 확인하는지)은 README에, 에이전트/내부 작업 규칙은 이 문서(CLAUDE.md)에 둔다.
- 문구가 실제 코드와 어긋나지 않는지 확인하고, 구현되지 않은 기능을 적지 말 것. 한계·미구현 항목은 숨기지 말고 "현재 한계"에 적는다.
