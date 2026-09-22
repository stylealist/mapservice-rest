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

## 남은 작업 (의도적으로 이번 범위에서 제외)

- **강제 적용 없음**: 게이트웨이나 mapservice-rest/scheduler 등 다른 서비스는 이 토큰을 요구하지 않는다. 전면 적용하려면 게이트웨이 전역 필터 + 프론트(`sj-lab-mapservice`) 로그인 화면이 함께 필요 — 별도 작업으로 진행할 것.
- **운영 배포 미구성**: `application-prod.yml` 없음, k8s 차트 없음, `AUTH_JWT_SECRET` k8s Secret 미생성.
- **역할/권한 개념 없음**: 지금은 로그인 성공 여부(`username`)만 다룸.

## 커밋·푸시 상태

- `sj-lab-authserver`: 초기 커밋 후 신규 GitHub 저장소에 푸시 완료(신규 저장소라 배포 파이프라인 영향 없음).
- `sj-lab-apigateway`, `mapservice-rest`: 로컬 커밋은 사용자 확인 후 진행 — 두 저장소 모두 push 시 Jenkins 빌드·k8s 배포가 자동으로 트리거되므로(system-architecture.md "배포 경로" 참고) push는 별도로 확인받는다.
