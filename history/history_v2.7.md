# history v2.7 — sj-lab-hub·sj-lab-mapservice 로그인 게이트 + SSO

- **날짜**: 2026-09-22
- **영향 저장소**: `sj-lab-authserver`(로그인 페이지·세션 쿠키 API 추가), `sj-lab-hub`(로그인 게이트), `sj-lab-mapservice`(로그인 게이트), `mapservice-rest`(문서만)
- **이전 버전**: [history_v2.6.md](history_v2.6.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 공유 로그인 페이지 | `GET /auth/login.html?redirect_uri=...` | 게이트웨이 경유 |
| 지도 프론트엔드 | http://localhost:4000 | 로컬 스택 |
| 허브 | http://localhost:3000 | `npm start`로 별도 기동 필요 |

## 실행한 프롬프트

```
일단 sj-lab-hub, sj-lab-mapservice 접속했을 때 로그인하고 진행되도록 해주고 sso 기능을 넣어서
한곳에서 로그인하면 다른 웹사이트에서도 로그인상태로 이용가능하도록 해줘
```

## 사전 확인 (AskUserQuestion)

| 질문 | 결정 |
|---|---|
| 적용 범위 | 사이트 전체를 로그인 후에만 볼 수 있게(전면 게이트) |
| 백엔드 API 강제 여부 | 아니오 — 프론트 화면만 게이트(이전 v2.6에서 명시적으로 미룬 결정 유지) |

## 설계

별도 SSO 라이브러리나 여러 사이트가 공유하는 쿠키 도메인 설정 없이, **리다이렉트 기반 중앙 로그인 페이지 + URL 해시로 토큰 전달** 방식을 택했습니다(CAS류 SSO의 단순화 버전).

```
1. hub/mapservice 접속 → localStorage에 유효한 토큰 없으면
   /auth/login.html?redirect_uri=<원래 주소> 로 리다이렉트
2. login.html(authserver가 서빙)이 자기 오리진의 세션 쿠키를 GET /auth/session 으로 확인
   - 있으면: 폼 없이 새 토큰 발급 → 3번
   - 없으면: 로그인 폼 → POST /auth/login(성공 시 세션 쿠키도 내려줌)
3. login.html 이 redirect_uri#auth_token=...&auth_expires=...&auth_username=... 로 되돌아감
4. 원래 사이트의 게이트 스크립트가 해시에서 토큰을 꺼내 자기 localStorage에 저장, 해시는 지움
```

이 방식을 고른 이유: hub(`localhost:3000`)·mapservice(`localhost:4000`)·authserver(`localhost:8100`)가 로컬에서 서로 다른 포트(=다른 오리진)라 쿠키를 직접 공유할 수 없습니다. 반면 로그인 페이지 자신의 세션 쿠키는 **같은 오리진으로의 최상위 탐색(top-level navigation)에서만** 쓰이므로 `SameSite=Lax`로 충분하고, `SameSite=None`+`Secure`가 요구하는 HTTPS 제약(로컬 http 환경에서 못 씀) 없이도 동작합니다.

## 만든 것

### `sj-lab-authserver`

| 파일 | 내용 |
|---|---|
| `src/main/resources/static/login.html` (신규) | 공유 로그인 페이지. `redirect_uri` 허용 오리진 검증(오픈 리다이렉트 방지), 세션 확인 → 폼 표시 → 로그인 → 해시로 복귀 |
| `AuthController` | `login()`이 세션 쿠키도 함께 내려주도록 수정. `GET /session`(쿠키로 조용히 재발급), `POST /logout`(쿠키 삭제) 신규 |
| `SecurityConfig` | **Spring Security 기본 로그아웃 필터를 명시적으로 꺼야 함을 실제로 겪음** — 꺼두지 않으면 `POST /logout`을 우리 컨트롤러보다 먼저 가로채 `/login?logout`으로 리다이렉트(302)해 버려, 클라이언트에서는 405로 보이는 혼란스러운 증상이 남 |
| `application.yml`/`application-local.yml` | `auth.cookie.name`(`sj_session`), `auth.cookie.secure`(로컬만 false) 추가 |
| `CLAUDE.md` | "SSO(사이트 간 자동 로그인)" 절 신설, 남은 작업에 single-logout 미구현 명시 |

### `sj-lab-hub`

| 파일 | 내용 |
|---|---|
| `public/index.html` | `<head>` 맨 위에 로그인 게이트를 **인라인 스크립트**로 추가(별도 파일은 `npm run build` 결과에 자동 복사되지 않아 프로덕션에서 깨짐 — `HtmlWebpackPlugin`의 `favicon` 옵션만 예외적으로 복사해 준다는 것을 확인 후 인라인으로 결정) |
| `src/App.js` | 우측 상단 로그아웃 버튼(`logoutButtonStyle`) + `outerStyle`에 `position: relative` 추가(버튼 기준 위치) |
| `CLAUDE.md` | 게이트 동작과 "인라인이어야 하는 이유" 기록 |

### `sj-lab-mapservice`

| 파일 | 내용 |
|---|---|
| `js/auth-gate.js` (신규) | 로그인 게이트(무빌드 정적 사이트라 별도 파일로 충분) |
| `index.html` | `<head>` 맨 위에 `<script src="js/auth-gate.js">` 추가, 헤더 nav에 로그아웃 버튼 추가 |
| `CLAUDE.md` | 게이트 동작 기록 |

### `mapservice-rest` (문서만)

`docs/system-architecture.md`에 SSO 흐름 설명 추가.

## 검증 결과

| 확인 | 방법 | 결과 |
|---|---|---|
| `POST /auth/login` 세션 쿠키 발급 | 게이트웨이 경유, `WebRequestSession`으로 쿠키 캡처 | `sj_session` 쿠키 HttpOnly로 저장됨 |
| `GET /auth/session`(쿠키만) | 같은 세션으로 Authorization 헤더 없이 호출 | 200, 새 토큰 발급 |
| `POST /auth/logout` | 쿠키 삭제 후 재호출 | `/auth/session`이 401로 전환 확인 |
| `GET /auth/login.html` | 게이트웨이 경유 | 200 text/html |
| 프론트 스크립트 문법 | `node --check`(mapservice `auth-gate.js`, hub·login.html 인라인 스크립트 추출) | 모두 통과 |
| hub 프로덕션 빌드 | `npm run build` | 성공, `build/index.html`에 게이트 스크립트 포함 확인 |
| hub 로컬 서버 | `npm start` 기동 후 `curl` | 200, 게이트 스크립트 포함 확인 |
| mapservice 로컬 서버 | 기존 로컬 스택(4000) | 200, `js/auth-gate.js` 200 확인 |

**검증하지 못한 것**: 이 세션에는 브라우저 자동화 도구가 없어, 실제 브라우저에서 "로그인 안 된 상태로 접속 → 리다이렉트 → 로그인 폼 입력 → 원래 사이트로 복귀 → 다른 사이트 접속 시 재로그인 없이 통과"까지 이어지는 전체 흐름은 사람이 직접 브라우저로 확인해야 합니다. 백엔드 API 동작과 각 스크립트의 문법·서빙 여부는 검증했지만, `localStorage`·`window.location` 조작이 실제 DOM에서 의도대로 동작하는지는 코드 리뷰 수준의 확인입니다.

## 겪은 문제

- **authserver 재기동 후 Eureka에 죽은 인스턴스가 남아 게이트웨이가 간헐적으로 500** — 이미 문서화된 함정(`docs/dev-environment.md`)과 동일. `Stop-Process -Force`로 강제 종료하면 Eureka에 정상 등록 해제(`DELETE /eureka/apps/...`)가 안 되므로, 재기동 후 죽은 인스턴스를 수동으로 지워야 했음.
- **Spring Security 기본 로그아웃 필터와의 충돌** — 위 "만든 것" 표 참고. `permitAll()`만으로는 막히지 않는, `authorizeHttpRequests`와 별도로 동작하는 필터라는 점이 함정이었음.

## 남은 작업

- 백엔드 API(mapservice-rest, scheduler) 강제 적용 — 이번에도 명시적으로 범위 밖.
- 진짜 single-logout(한 사이트 로그아웃 시 다른 사이트도 즉시 로그아웃) — 지금은 토큰이 자연 만료(12시간)될 때까지 남음.
- 사람이 실제 브라우저로 전체 SSO 흐름(로그인 → 리다이렉트 복귀 → 다른 사이트에서 재로그인 없이 통과 → 로그아웃) 확인 필요.
