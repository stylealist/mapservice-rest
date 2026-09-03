# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

**요청 흐름**: `WfsController`(REST 엔드포인트) → `WfsService` / `WfsServiceImpl` → `WfsMapper`(자바 쪽 SQL이 없는 MyBatis `@Mapper` 인터페이스) → `src/main/resources/mapper/wfs-geojson.xml` → PostgreSQL/PostGIS.

핵심 포인트: 각 매퍼 메서드의 SQL은 DB 뷰(예: `map.v_bus_stop_info_geojson`)에서 이미 완성된 `geojson` 텍스트 컬럼을 그대로 select할 뿐이며, GeoJSON 조립은 자바가 아니라 DB에서 이루어집니다. 서비스/컨트롤러 계층은 이 문자열을 가공 없이 그대로 통과시킵니다. 새 지도 레이어를 추가하려면: `geojson` 컬럼을 만드는 DB 뷰를 추가한 뒤, `WfsMapper`, `wfs-geojson.xml`, `WfsService`/`WfsServiceImpl`, `WfsController` 엔드포인트에 동일한 패턴으로 항목을 추가하면 됩니다 — 기존 레이어(편의점, 버스정류장, CCTV, 약국, 병원, 관공서)가 모두 이 네 곳에 똑같은 패턴으로 구현되어 있습니다.

매퍼 XML은 `application.yml`의 `mybatis.mapper-locations: classpath*:mapper/**/*.xml` 설정으로 자동 스캔되며, `MapServiceRestApplication`에 `@MapperScan("com.example.mapservice.mapper")`가 선언되어 있습니다.

**설정 계층**: `application.yml`이 기본/운영에 가까운 설정(서버 포트, context-path, datasource, mybatis, actuator)을 담당합니다. `application-local.yml`은 Eureka `defaultZone`을 `localhost:8761`로 덮어쓰고, 로컬 환경에는 게이트웨이가 prefix를 벗겨주지 않으므로 Swagger UI URL에서 `/map` prefix를 제거하도록 재정의합니다.

**보안**: `SecurityConfig`는 CSRF를 비활성화하고 인메모리 사용자 1명만 정의하며, 현재 WFS 엔드포인트에 대한 접근 제어는 없습니다. `MapServiceRestController`(`/welcome`, `/message`, `/check`)와 `HelloWorldController`(`/hello-world*`)는 지도 기능과 무관한 잔존 보일러플레이트/데모 엔드포인트이므로 핵심 로직으로 취급하지 마세요.

**i18n**: `messages*.properties` + `MessageSource`는 `HelloWorldController#helloWorldInternationalized`에서만 사용되며, 다른 곳에는 쓰이지 않습니다.

**예외 처리**: `CustomizedResponseEntityExceptionHandler`가 전역 `@ControllerAdvice`로 존재하지만, `WfsController`의 각 메서드는 예외를 전파하지 않고 자체적으로 catch해서 `e.printStackTrace()`만 호출하고 빈 문자열을 반환합니다. 즉 WFS 엔드포인트가 500이 아니라 HTTP 200 + 빈 바디를 반환할 수 있으니 디버깅 시 유의하세요.

## 변경 시 참고사항

- `application.yml`에 운영 DB 비밀번호가 평문으로 커밋되어 있습니다 — 여기에 추가로 민감정보를 넣지 말고, datasource 설정을 건드릴 경우 자격 증명을 외부화하는 방향을 우선 고려하세요.
- `bean/AdminUser.java`, `bean/AdminUserV2.java`와 `NewSwaggerConfig`의 OpenAPI `/users/**`, `/admin/**` 그룹은 대응하는 컨트롤러가 없는 스캐폴딩입니다 — 미완성/참고용 코드로 보이며 실제 앱에는 연결되어 있지 않습니다.
