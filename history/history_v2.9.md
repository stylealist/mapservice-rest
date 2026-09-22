# history v2.9 — 로그인 게이트 재적용 후속: 로그아웃·접속자 표시·체험용 계정·Secret 정리

- **날짜**: 2026-09-22
- **영향 저장소**: `sj-lab-authserver`, `sj-lab-hub`, `sj-lab-mapservice`, `sj-lab-k8s-manifests`, `mapservice-rest`(문서)
- **이전 버전**: [history_v2.8.md](history_v2.8.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 운영 허브 | https://sj-lab.co.kr | 로그인 게이트 재적용됨 |
| 운영 지도 | https://sj-lab.co.kr/map/ | 허브와 같은 오리진의 하위 경로 |
| 운영 로그인 페이지 | https://api.sj-lab.co.kr/auth/login.html | 체험용 계정 버튼 포함 |
| Secret 정리 문서 | `docs/k8s-secrets.md` | 이번에 신설 |

## 실행한 프롬프트

```
실서버에서 로그인시스템을 사용할 수 있도록 모두 commit push 진행해줘
로컬에서도 sj-lab-hub에서 sj-lab-mapservice로 이동 할 수 있도록 해줘
지금 오류가 있는데 로그인은 되는데 로그아웃은 안되고있어
sj-lab-mapservice의 로그아웃버튼이 지도,소개,연락처랑 붙어있지않았으면 좋겠고 sj-lab-hub처럼 로그아웃버튼이 따로 있으면 좋겠어 그리고 로그아웃 버튼 왼쪽에 접속자 아이디가 나오도록 해줘
mapservice쪽에서 로그아웃 버튼이 깨져서 나와
로그아웃 버튼옆에 아이디만 나오니까 뭔가 아이디인지 모르겠어 일반적인 로그인시에 아이디 표시 문구로 해줄수있어?
테스트 계정으로 로그인 넣어줘
아이디 demo · 비밀번호 ... 를 굳이 공개하고싶지않아
서버 처리 방식으로 바꿔줘
push해줘 그리고 Secret 이름들과 용도에 대해서 정리한 파일이 있으면 좋겠어 확인 명령어도
```

## 작업 내용

| 항목 | 원인·배경 | 조치 |
|---|---|---|
| 로그인 게이트 운영 재적용 | v2.7 장애(authserver 없이 강제 리다이렉트) 이후 롤백 상태였음 | authserver 운영 동작과 로그인 페이지의 허용 오리진을 먼저 확인한 뒤 hub·mapservice 롤백을 되돌림(revert of revert). mapservice의 운영 위치가 `sj-lab.co.kr/map/`(허브와 같은 오리진)임을 확인 |
| 로컬 허브 → 지도 이동 | 로컬은 hub(3000)·mapservice(4000)가 다른 포트라 상대경로 `/map`으로 못 감 | hub `resolveFeaturePath()` — 로컬이면 `http://localhost:4000`, 운영은 상대경로 유지 |
| **로그아웃이 안 됨** | 프론트 로그아웃은 자기 localStorage만 지우고 로그인 페이지로 보내는데, 로그인 페이지가 살아있는 세션 쿠키로 곧바로 새 토큰을 발급해 되돌려 보냄 | 로그아웃 시 `login.html?...&logout=1`로 보내고, 로그인 페이지가 `POST /auth/logout`으로 쿠키를 지운 뒤 폼 표시 |
| mapservice 로그아웃 버튼 위치 | 버튼이 지도·소개·연락처 탭(`.nav-btn`) 안에 있었고, 그 탓에 `ui.js`의 페이지 전환 핸들러에도 걸려 있었음 | 헤더 오른쪽 별도 `.user-box`로 분리, hub와 같은 알약형 버튼 |
| 접속자 표시 | 아이디만 있어 무엇인지 알기 어려움 | 두 사이트 모두 "사람 아이콘 + **아이디**님" 형식 |
| **헤더가 깨져 보임** | 로컬(Python)·운영(nginx) 모두 `Cache-Control` 없이 `Last-Modified`만 내려줘, 브라우저가 옛 `header.css`를 재검증 없이 사용 → 새 HTML + 옛 CSS | 바뀐 CSS/JS 참조에 `?v=` 캐시 무효화 쿼리. mapservice `CLAUDE.md`에 "파일 수정 시 버전 올릴 것" 규칙 추가. hub의 고정 파일명 `bundle.js`도 같은 위험이 있으나, 운영 배포 잡이 파일명에 의존하는지 확인할 수 없어 `contenthash` 적용은 보류 |
| 체험용(데모) 계정 | 포트폴리오 방문자가 바로 들어올 수 있게 | 로그인 페이지에 "체험용 계정으로 로그인" 버튼 |
| **데모 비밀번호 노출** | 처음엔 버튼이 페이지 JS에 담긴 아이디·비밀번호로 제출 → 소스 보기·public 저장소에서 노출 | `POST /auth/login/demo` 신설 — 서버가 `AUTH_DEMO_USERNAME`/`AUTH_DEMO_PASSWORD`(k8s Secret `auth-demo-credentials`, optional)로 로그인. 페이지·저장소에서 값 제거. 옛 비밀번호는 git 기록에 남으므로 **QFieldCloud에서 데모 비밀번호를 교체**(새 값 로그인 200, 옛 값 401 확인) |
| Secret 정리 | 운영 비밀값의 이름·용도를 한곳에서 보고 싶음 | `docs/k8s-secrets.md` 신설 — k8s Secret 4종·Jenkins Credential 3종의 용도, 없을 때 증상, 확인·생성·변경 명령어(값은 적지 않음) |

## 검증

| 확인 | 결과 |
|---|---|
| 로컬 헤더 화면(헤드리스 Chrome 캡처) | hub·mapservice 모두 "👤 아이디님 [로그아웃]" 정상 표시 |
| 데모 엔드포인트 — 환경변수 없음 | `POST /auth/login/demo` 503, 일반 로그인은 200(영향 없음) |
| 데모 엔드포인트 — 환경변수 있음 | 200, `username=demo` |
| `sj-lab-authserver` 차트 | `helm lint`·`helm template` 통과, 데모 Secret은 `optional: true` |
| 로그인 페이지 스크립트 | `node --check` 통과, 저장소 파일에 데모 비밀번호 없음(`git grep` 확인) |

## 남은 작업

- 운영에서 로그아웃 → 로그인 폼 표시, 체험용 버튼 로그인을 브라우저로 확인(authserver Jenkins 배포 후).
- hub `bundle.js` 캐시 문제 — 운영 배포 방식 확인 후 `contenthash` 적용 여부 결정.
- 한 사이트 로그아웃 시 다른 사이트는 토큰 만료(12시간)까지 로그인 유지(single-logout 미구현).
- `sj-lab-scheduler`의 CCTV 기동 시 자동 수집 변경(v2.5)이 아직 커밋되지 않음.
