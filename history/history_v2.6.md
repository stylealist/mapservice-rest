# history v2.6 — sj-lab-authserver 신설 (QFieldCloud 계정 기반 로그인 서버)

- **날짜**: 2026-09-22
- **영향 저장소**: `sj-lab-authserver`(신규, GitHub 저장소도 신설), `sj-lab-apigateway`(라우팅 추가), `mapservice-rest`(문서만)
- **이전 버전**: [history_v2.5.md](history_v2.5.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| GitHub 저장소 | https://github.com/stylealist/sj-lab-authserver | public |
| 로그인 API(게이트웨이 경유) | `POST /auth/login`, `GET /auth/me` | `{ "username","password" }` → JWT 발급 |

## 실행한 프롬프트

```
/orchestration 현재 내 springboot 버전에 맞게 spring서버를 하나 생성해서 로그인서버를 만드려고하는데
기본 로그인 계정은 qfieldcloud의 로그인 시스템을 붙여서 사용하고 싶어 로그인 서버 만들어서 github까지
만들어줘 그리고 필요하다면 discovery 서버, apigateway등에도 연결해서 사용해줘
```

## 사전 확인 (AskUserQuestion)

새 GitHub 저장소 생성·보안 아키텍처 결정은 되돌리기 어려워 진행 전 3가지를 확인했습니다.

| 질문 | 결정 |
|---|---|
| 저장소 이름 | `sj-lab-authserver` |
| GitHub 공개 범위 | Public |
| 적용 범위 | **로그인 서버 + 게이트웨이 라우팅만 우선 구축** — 기존 API에는 토큰 검증을 강제하지 않음(프론트에 로그인 화면이 아직 없어 전면 강제 시 서비스가 깨짐) |

## 만든 것

### 신규 저장소 `sj-lab-authserver`

Spring Boot 3.3.2 / Java 17 / Spring Cloud 2023.0.3 (다른 sj-lab 백엔드와 동일 버전). Eureka에 `SJ-LAB-AUTHSERVER`로 등록.

- `POST /auth/login {username,password}` — 자체 회원 DB 없이, 매 로그인 요청을 QFieldCloud `POST /api/v1/auth/login/`에 그대로 위임 검증(`mapservice-rest`의 `QfieldMediaServiceImpl`과 같은 API 계약 재사용). 성공하면 QFieldCloud 토큰은 버리고, 이 서버가 서명하는 sj-lab 전용 JWT를 새로 발급. 실패(400/401)는 401, 그 외 업스트림 오류는 502.
- `GET /auth/me` — `Authorization: Bearer <token>`으로 발급한 토큰의 유효성을 확인.
- `JwtService`: HS256, `AUTH_JWT_SECRET` 환경변수로 서명 키 주입(저장소가 public이라 실제 비밀값은 코드에 없음, 로컬 전용 기본값만 명시).
- `SecurityConfig`: 이 서버 자신은 모든 요청을 permitAll(로그인 엔드포인트라 당연히 열려 있어야 함).
- `CLAUDE.md`에 "현재 범위와 남은 작업" 섹션을 명시 — 다른 서비스에 토큰 검증 강제 없음, 역할/권한 개념 없음, 리프레시 토큰 없음, 운영 프로파일 없음을 분명히 적어 다음 작업자가 마음대로 확장하지 않도록 함.

### `sj-lab-apigateway`

`application.yml`에 라우트 추가:
```yaml
- id: sj-lab-authserver
  uri: lb://SJ-LAB-AUTHSERVER
  predicates:
    - Path=/auth/**
  filters:
    - name: CustomFilter
    - PreserveHostHeader
```
다른 서비스와 동일한 패턴(`CustomFilter` + `PreserveHostHeader`). CORS `globalcors`는 이미 전체 경로(`/**`)에 적용돼 있어 별도 수정 불필요.

### `sj-lab-discoveryServer`

**코드 변경 없음.** Eureka는 등록하는 쪽(클라이언트)만 설정하면 되므로, 새 서비스를 위한 디스커버리 서버 자체 수정은 필요하지 않았다. 실제로 `local` 프로파일로 기동하자 별도 조치 없이 `SJ-LAB-AUTHSERVER`로 정상 등록됨을 확인.

### `mapservice-rest` (문서만)

`docs/system-architecture.md`(계층 구성도, 저장소 표, API 계약에 `/auth/**` 절 추가), `docs/dev-environment.md`(저장소 경로 표, additionalDirectories 안내), `.claude/settings.local.json`의 `additionalDirectories`에 경로 추가(로컬 전용, git 비추적).

## 검증 결과

로컬 스택(Eureka·mapservice-rest·게이트웨이·프론트) 기동 상태에서 authserver를 `local` 프로파일로 추가 기동, 게이트웨이를 재빌드·재기동해 새 라우트를 반영한 뒤 실제 QFieldCloud 계정(기존에 미디어 중계용으로 쓰던 서비스 계정)으로 엔드투엔드 검증:

| 확인 | 결과 |
|---|---|
| `SJ-LAB-AUTHSERVER` Eureka 등록 | 확인 |
| `POST /auth/login`(게이트웨이 경유, 올바른 비밀번호) | 200, JWT 발급, `username` 정확히 반환 |
| `GET /auth/me`(발급받은 토큰으로) | 200, 같은 `username` 반환 |
| `POST /auth/login`(잘못된 비밀번호) | 401 |
| `mvnw.cmd test`(authserver) | 컨텍스트 로딩 스모크 테스트 통과 |

## 보안 리뷰 및 수정

일반 리뷰 에이전트에게 로그인 서버 특유의 관점(비밀번호 로그 유출, 시크릿 취급, 401/502 응답의 정보 노출, 타이밍/계정 존재 여부 유추, JJWT 키 길이, `/auth/me` 헤더 누락 처리)으로 코드 리뷰를 요청했습니다. 발견 사항과 조치:

| 발견 | 심각도 | 조치 |
|---|---|---|
| **JWT 시크릿 기본값 무방비** — `AUTH_JWT_SECRET`을 안 넣고 배포해도 조용히 기동돼, GitHub에 공개된 기본 키로 서명할 수 있었음 | 배포 전 반드시 수정 | `JwtService`에 `@PostConstruct` 검증 추가: ① 시크릿이 32바이트 미만이면 기동 실패 ② `local` 프로파일이 아닌데 기본값이면 기동 실패. `Environment.acceptsProfiles()`로 실제 활성 프로파일을 확인(처음엔 `@Value("${spring.profiles.active}")`로 시도했다가 테스트의 `@ActiveProfiles`에서는 이 프로퍼티가 채워지지 않는 것을 발견해 수정) |
| **로그인 요청 JSON 수동 조립** — 비밀번호에 제어문자(`\n`,`\t`)가 있으면 깨진 JSON이 만들어짐(보안 유출은 아니고 기능 버그) | 낮음이지만 실제 버그 | 수동 문자열 포맷 대신 Jackson `ObjectMapper.writeValueAsString()`으로 직렬화하도록 교체, `escapeJson` 헬퍼 제거 |
| 비밀번호 로그 유출 | - | 없음(확인만, 조치 불필요) — 로그에는 상태 코드/메시지만 남음 |
| 401/502 응답의 계정 존재 여부 유추 가능성 | - | 없음(확인만) — QFieldCloud 400/401을 구분 없이 같은 401 메시지로 매핑 |
| `/auth/login`에 레이트 리미팅 없음 | 남은 과제로 기록 | 이번엔 고치지 않음 — 로컬/사내망 밖으로 열기 전에 추가하도록 `CLAUDE.md`에 명시 |

검증: `mvnw.cmd test` 통과(수정 후 `local` 프로파일이 아니면 기동 실패하므로 스모크 테스트에 `@ActiveProfiles("local")` 추가), 재기동 후 게이트웨이 경유 로그인·`/auth/me`·잘못된 비밀번호 401을 다시 확인(게이트웨이 로드밸런서 캐시가 재기동 직후 잠시 죽은 인스턴스를 가리켜 첫 재검증은 500 — 몇 초 후 재시도로 정상 확인, 문서에 이미 있는 "재기동 후 Eureka 캐시 지연" 함정과 동일).

## 남은 작업 (의도적으로 이번 범위에서 제외)

- **강제 적용 없음**: 게이트웨이나 mapservice-rest/scheduler 등 다른 서비스는 이 토큰을 요구하지 않는다. 전면 적용하려면 게이트웨이 전역 필터 + 프론트(`sj-lab-mapservice`) 로그인 화면이 함께 필요 — 별도 작업으로 진행할 것.
- **운영 배포 미구성**: `application-prod.yml` 없음, k8s 차트 없음, `AUTH_JWT_SECRET` k8s Secret 미생성.
- **역할/권한 개념 없음**: 지금은 로그인 성공 여부(`username`)만 다룸.
- **레이트 리미팅 없음**: `/auth/login`이 매 요청을 실제 QFieldCloud로 전달하므로, 외부에 열기 전 속도 제한이 필요.

## 커밋·푸시 상태

- `sj-lab-authserver`: 초기 커밋 후 신규 GitHub 저장소에 푸시 완료(신규 저장소라 배포 파이프라인 영향 없음).
- `sj-lab-apigateway`, `mapservice-rest`: 로컬 커밋은 사용자 확인 후 진행 — 두 저장소 모두 push 시 Jenkins 빌드·k8s 배포가 자동으로 트리거되므로(system-architecture.md "배포 경로" 참고) push는 별도로 확인받는다.
