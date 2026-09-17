# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## sj-lab 통합 허브

이 저장소는 sj-lab 저장소들을 넘나드는 작업(프론트엔드 `sj-lab-mapservice` + 이 백엔드 + 게이트웨이)을 총괄하는 기준 저장소입니다. 디스커버리(`sj-lab-discoveryServer`), 수집 배치(`sj-lab-scheduler`), `fast-api-ai`도 이 세션에서 함께 다룹니다. 총괄 Claude 세션은 여기서 띄우고, MCP 설정(`.mcp.json`), 로컬 비밀값(`.claude/settings.local.json`), Bash 가드 훅(`.claude/hooks/guard.sh`), DB 분석 문서(`docs/analysis/`)를 이 저장소에서 관리합니다. 프론트엔드 코드 규칙은 `C:\vscode_develop\sj-lab-mapservice\CLAUDE.md`를 따릅니다.

- @docs/system-architecture.md — DB → 백엔드 → Eureka → 게이트웨이 → 프론트 전체 구조, API 계약 표, 저장소를 넘나드는 변경 체크리스트, 총괄 세션에서 다른 저장소를 다룰 때의 규칙
- @docs/dev-environment.md — 로컬 저장소 경로, 포트·라우팅(8100=게이트웨이, 4000=프론트, 8761=Eureka), CORS, 워커 운영 시 주의
- @docs/mcp.md — GitHub/DB MCP 설정과 비밀값 관리 규칙
- `docs/analysis/sjlab-dev-db-check.md`, `docs/analysis/db-analysis.md` — 개발 DB 연결·권한·스키마 점검 및 인덱스·뷰·데이터 품질 분석

공통 규칙:
- 답변은 한글로 할 것.
- 코드 추가·수정 중 CLAUDE.md 또는 docs에 반영해야 할 내용이면 코드 변경 직후 바로 추가할 것.
- 함수·변수 이름은 카멜 형식으로 지을 것.
- 바로 commit, push하지말고 한번 물어본후에 진행할것
- 게이트웨이·디스커버리·scheduler·fast-api-ai·프론트엔드·hub·k8s-manifests·qfieldsync·infra-manage-app 저장소 파일을 수정하기 전에 그 저장소의 `CLAUDE.md`를 먼저 Read할 것(이 세션에 자동 로드되지 않음). git 작업은 `git -C <경로>`로 저장소별로 할 것.
- API 경로·응답 형식을 바꾸면 백엔드와 프론트(`map-wfs.js`/`map-facility.js`)를 같은 작업에서 함께 수정하고 `docs/system-architecture.md`의 API 계약 표를 갱신할 것.
- `.claude/hooks/guard.sh`가 `git reset --hard`, `git push --force`, 그리고 `claude` 문자열이 들어간 Bash 명령을 차단합니다. 차단되면 우회하지 말고 다른 도구(Read/Grep/PowerShell)로 해결할 것.

작업 규칙(2026-09-16 추가):
- **작업 범위**: `docs/dev-environment.md`의 "로컬 저장소 경로" 표에 명시된 저장소 안에서만 작업할 것. 표에 없는 경로를 읽거나 고쳐야 하면 먼저 사용자에게 확인하고, 승인되면 그 표와 `additionalDirectories`에 추가할 것.
- **DB**: 기본은 조회(SELECT)만 할 것. 사용자가 특정 스크립트를 콕 집어 "실행해줘"라고 지시한 경우에만 예외로 실행하되, ① 실행할 SQL을 먼저 저장소에 스크립트 파일로 남기고 ② 대상 DB·계정을 밝힌 뒤 ③ 실행 결과와 함께 history에 기록할 것. 그 외에는 `CREATE`/`ALTER`/`DROP`(DDL), `GRANT`/`REVOKE`(DCL), `INSERT`/`UPDATE`/`DELETE`(DML)를 임의로 실행하지 말 것 — 필요한 SQL은 실행하지 말고 사용자에게 제안만 할 것. scheduler를 로컬에서 띄우면 cron 배치가 DB에 적재하므로 이것도 사용자 확인 후에.
- **파일 삭제**: 파일을 지우지 말고 `trash/<YYYY-MM-DD>/` 아래에 원래 경로 구조를 유지한 채 옮길 것(예: `trash/2026-09-16/docs/old.md`). 옮긴 파일은 그 버전의 history 문서에 기록할 것.
- **git push**: 자동으로 push하지 말 것. 사용자가 명시적으로 요청할 때만 push하며, 커밋은 저장소별로 `git -C <경로>`로 할 것.
- **변경 기록(history)**: 파일을 실제로 변경한 작업마다 `history/history_v<major>.<minor>.md`를 새로 만들 것(조회·질문만 한 턴은 기록하지 않음). 내용은 사용자가 입력한 프롬프트 원문, 변경된 결과물(파일별 요약), 접속 URL 링크를 포함할 것. 직전 버전에서 minor를 1 올리고, 구조가 바뀌는 큰 작업이면 major를 올릴 것.
- **공유 웹사이트**: history 문서를 추가·수정하면 같은 작업에서 두 곳을 함께 갱신할 것.
  1. `history/web/artifact.html` — 팀에 링크로 공유하는 발행 페이지의 원본. 버전 패널을 추가한 뒤 **같은 URL로 다시 발행**해야 링크가 유지됨(발행 주소: `https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9`). 발행은 외부 서비스에 내용을 올리는 행위이므로 민감 정보를 싣지 말 것.
  - 작업로그는 탭으로 관리되면 한탭에 15개의 로그가 포함되도록 한다.(예시 : v1.0 ~ v.1.14, v1.15 ~ v.1.29 이런식으로 진행)
    - 페이지 스크립트(`chipsPerGroup = 15`)가 로드 시 버전 칩을 순서대로 15개씩 다시 묶고 탭 이름(`v첫 – v끝`)도 만든다. 새 버전은 마지막 묶음 끝에 칩만 추가하면 되며, 마크업의 묶음도 가능하면 15개 단위로 맞춰 둘 것(스크립트가 꺼진 환경 대비).
  2. `history/web/index.html` — 오프라인용 요약 페이지. 외부 CDN·빌드 도구 없이 단일 HTML로 유지할 것.
  - 새 history 문서의 접속 URL 표 맨 위에 발행 페이지 주소를 넣을 것
- **AI에이전트 오케스트레이션**: 오케스트레이션을 진행 할 때 최종 검증은 반드시 Claude로 진행해줘

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

핵심 포인트:
- 기존 레이어(편의점, 버스정류장, CCTV, 약국, 병원, 관공서): DB 뷰(예: `map.v_bus_stop_info_geojson`)에서 이미 완성된 `geojson` 텍스트 컬럼을 그대로 select할 뿐이며, GeoJSON 조립은 DB에서 이루어집니다. 새 레이어 추가 시 DB 뷰를 생성할 수 있다면 `geojson` 컬럼을 가진 뷰를 추가한 뒤 동일 패턴으로 4개 계층(`WfsMapper`, `wfs-geojson.xml`, `WfsService`/`WfsServiceImpl`, `WfsController`)에 메서드를 추가합니다.
- WFS 레이어의 화면 영역 조회: `WfsController`의 6개 엔드포인트는 `bbox`(EPSG:3857 `minX,minY,maxX,maxY`)와 `limit`을 선택 파라미터로 받습니다. `bbox`가 있으면 뷰가 아니라 **원본 테이블**을 조회하는 `*ByBbox` 문(`wfs-geojson.xml`)을 타고, 없으면 기존처럼 뷰의 전국 데이터를 그대로 내려줍니다(구버전 프론트 호환용). `*ByBbox` 문은 뷰와 같은 중복 제거·properties 구성을 옮겨 적은 것이므로 **scheduler의 뷰 정의가 바뀌면 이 XML도 함께 고쳐야** 결과가 어긋나지 않습니다. 상한을 넘을 때는 bbox 를 격자로 나눠 칸마다 하나씩 뽑아(`row_number`) 화면 전체에 고르게 퍼진 표본을 내려줍니다 — 이 정렬을 단순 `limit`으로 바꾸면 화면 한쪽만 채워집니다.
- 읽기 전용 DB 등 뷰 생성이 불가능한 경우: `qfield-facility.xml` 패턴을 따릅니다. XML 내 SQL에서 `json_build_object`, `json_agg`, `to_jsonb`, `ST_AsGeoJSON` 등을 활용해 DB 레벨에서 GeoJSON/JSON 문자열로 직접 조립해 반환합니다. 파라미터는 반드시 MyBatis `#{}` 바인딩을 사용합니다.
- 시설물 공간 쿼리 및 행정구역 필터: `qfield.facility_total_view`의 EPSG:3857 점 좌표와 `public.g_emd`의 EPSG:4326 경계를 조인할 때, `LEFT JOIN LATERAL`과 `ST_Intersects(e.geom, ST_Transform(f.geom, 4326))` (LIMIT 1)로 `g_emd`의 GIST 인덱스를 활용합니다. 행정구역 코드는 접두어 계층 구조(sido 2자리, sgg 5자리, emd 8자리)이므로 `emd_cd LIKE code || '%'` 단일 조건으로 고속 필터링합니다 (g_sido 폴리곤 직접 조인이나 ST_MakeValid는 지양).
- 행정구역 BBOX 조회: 폴리곤 전체 좌표를 변환하지 않고 `ST_Transform(ST_Envelope(geom), 3857)`로 BBOX만 변환하여 `[minX, minY, maxX, maxY]`를 계산합니다.
- 첨부 파일 중계: 시설물 사진·음성·영상은 DB에 QField 프로젝트 내 상대 경로만 있고 원본은 인증이 필요한 QFieldCloud에 있습니다. `QfieldMediaService`가 토큰 로그인 → 프로젝트 식별 → 파일 다운로드를 거쳐 전달하며, **요청 경로가 그 시설물의 첨부인지 DB로 검증한 뒤에만** 응답합니다(아니면 403). 계정은 `QFIELD_USERNAME`/`QFIELD_PASSWORD` 환경변수로만 주입하고 `application.yml`에 적지 마세요. 로컬은 `scripts/local-stack.ps1`이 `.claude/settings.local.json`의 `env`에서 읽어 넣고, 운영은 `qfield-credentials` Secret에서 받습니다. 값이 없으면 **이 엔드포인트만 503**이 되므로, 첨부만 안 나온다면 계정 주입부터 확인하세요.
- 설정 테이블: 지도 시설물 아이콘은 `qfield.facility_icon`에서 관리하며 `GET /map/qfield/facility-icons`(`getFacilityIcons`)로 내려줍니다. 생성 스크립트는 `db/qfield_facility_icon.sql`이고 **DDL 실행은 담당자가 직접** 합니다. 테이블이 없거나 조회가 실패하면 서비스가 예외를 삼키고 `null`을 반환해 컨트롤러가 빈 배열(`[]`)을 내려주며, 프론트는 내장 기본 아이콘으로 동작합니다 — 이 경로는 의도된 것이므로 예외를 다시 던지도록 바꾸지 마세요.
- 예외 및 검증: `QfieldFacilityController`는 파라미터 유효성 검증 실패 시 HTTP 400, 시설물 미존재 시 HTTP 404를 명확히 반환하며, 500 오류를 삼키지 않고 정확한 응답 코드를 제공합니다.

매퍼 XML은 `application.yml`의 `mybatis.mapper-locations: classpath*:mapper/**/*.xml` 설정으로 자동 스캔되며, `MapServiceRestApplication`에 `@MapperScan("com.example.mapservice.mapper")`가 선언되어 있습니다.

**설정 계층**: `application.yml`이 기본/운영에 가까운 설정(서버 포트, context-path, datasource, mybatis, actuator)을 담당합니다. `application-local.yml`은 Eureka `defaultZone`을 `localhost:8761`로 덮어쓰고, 로컬 환경에는 게이트웨이가 prefix를 벗겨주지 않으므로 Swagger UI URL에서 `/map` prefix를 제거하도록 재정의합니다.

**보안**: `SecurityConfig`는 CSRF를 비활성화하고 인메모리 사용자 1명만 정의하며, 현재 WFS 엔드포인트에 대한 접근 제어는 없습니다. `MapServiceRestController`(`/welcome`, `/message`, `/check`)와 `HelloWorldController`(`/hello-world*`)는 지도 기능과 무관한 잔존 보일러플레이트/데모 엔드포인트이므로 핵심 로직으로 취급하지 마세요.

**i18n**: `messages*.properties` + `MessageSource`는 `HelloWorldController#helloWorldInternationalized`에서만 사용되며, 다른 곳에는 쓰이지 않습니다.

**예외 처리**: `CustomizedResponseEntityExceptionHandler`가 전역 `@ControllerAdvice`로 존재하지만, `WfsController`의 각 메서드는 예외를 전파하지 않고 자체적으로 catch해서 `e.printStackTrace()`만 호출하고 빈 문자열을 반환합니다. 즉 WFS 엔드포인트가 500이 아니라 HTTP 200 + 빈 바디를 반환할 수 있으니 디버깅 시 유의하세요.

## 변경 시 참고사항

- `application.yml`에 운영 DB 비밀번호가 평문으로 커밋되어 있습니다 — 여기에 추가로 민감정보를 넣지 말고, datasource 설정을 건드릴 경우 자격 증명을 외부화하는 방향을 우선 고려하세요.
- `bean/AdminUser.java`, `bean/AdminUserV2.java`와 `NewSwaggerConfig`의 OpenAPI `/users/**`, `/admin/**` 그룹은 대응하는 컨트롤러가 없는 스캐폴딩입니다 — 미완성/참고용 코드로 보이며 실제 앱에는 연결되어 있지 않습니다.
