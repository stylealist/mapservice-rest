# history v2.56 — API 활용 페이지 3단계: 내 API 키와 사용량

- **날짜**: 2026-09-29
- **영향 저장소**: `sj-lab-openapi`, `sj-lab-openapi-web`, `mapservice-rest`(문서·기록)
- **이전 버전**: [history_v2.55.md](history_v2.55.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 작업 로그 |
| 활용 페이지(로컬) | http://localhost:4100 | 키 영역 포함 |
| 허브(로컬) | http://localhost:3000 | |

## 실행한 프롬프트

```
커밋 푸시하고 3단계 진행해줘
```

## 만든 것 — 키를 발급받아 쓰고, 얼마나 썼는지 보기

| 주소 | 하는 일 |
|---|---|
| `GET /open-api/keys/status` | 키 기능을 쓸 수 있는 상태인지(로그인 없이 확인) |
| `POST /open-api/keys` | 키 발급 — **원문은 이때 한 번만** 보여 줍니다 |
| `GET /open-api/keys` | 내 키 목록 · 오늘 사용량 · 한도 · 마지막 사용 |
| `DELETE /open-api/keys/{keyId}` | 키 지우기 |

호출할 때 `X-API-Key` 헤더(또는 `?apiKey=`)를 붙이면 **사용량이 기록되고 하루 한도**가 적용됩니다.
키 없이도 부를 수 있고, 그때는 기록하지 않습니다.

화면에는 "내 API 키" 영역이 생겨 발급·삭제·사용량 확인을 하고, **고른 키는 실행해 보기와 샘플 코드에 함께** 들어갑니다.

### 안전하게 만든 부분

- **키 원문을 저장하지 않습니다.** DB에는 SHA-256 해시와 앞 8자리만 남습니다 — 잃어버리면 새로 발급해야 합니다.
- 발급받은 키는 **브라우저에도 저장하지 않습니다**(화면에서만 들고 있음). 같은 PC를 쓰는 다른 사람에게 남지 않게.
- 로그인 확인은 기존 로그인 서버(`/auth/me`)에 맡겼습니다. 토큰 검증 코드를 또 만들지 않았습니다.
- `apiKey` 쿼리는 데이터 서버로 넘기지 않습니다(다른 서버 로그에 키가 남지 않도록).
- 남의 키는 보이지도, 지워지지도 않습니다(계정을 조건에 넣고 조회·삭제).

### 표가 없어도 서비스는 돕니다

키·사용량 표는 `db/openapi_api_key.sql`, `db/openapi_api_usage.sql`로 만드는데, **DDL 실행은 담당자 몫**입니다.
표가 없거나 기능이 꺼져 있으면 **키 API만 503**이고 공개 API 조회는 그대로 됩니다. 표가 생기면 재기동 없이
60초 안에 알아서 인식합니다.

## 검증 (로컬, 포트 8110 + 헤드리스 Chrome)

| 요청 | 결과 |
|---|---|
| 키 기능 상태 | 200 · `ready: false` (표 없음 → 안내 문구) |
| 키 목록 · 발급 (로그인 없음) | **401** `LOGIN_REQUIRED` |
| 키 목록 (잘못된 토큰) | **401** (로그인 서버가 판정) |
| 키 없이 조회 | **200** (공개 API 그대로) |
| 가짜 키로 조회 (헤더 · 쿼리 둘 다) | **503** `NOT_CONFIGURED` |
| 화면 | "내 API 키" 영역 표시, 콘솔 오류 0건, 빌드 성공 |
| 테스트 | 8건 통과(카탈로그 4 + 키 4) |

처음엔 `Content-Type` 없이 키 발급을 부르면 415가 나와 진짜 원인(401)이 가려졌습니다. 본문을 문자열로 받아
직접 읽도록 바꿔 해결했습니다. DB 연결 실패 로그도 원인 메시지까지 남기도록 고쳤습니다 —
"표가 없음"과 "DB에 못 붙음"은 대응이 다르기 때문입니다.

## 바뀐 파일

| 저장소 | 내용 |
|---|---|
| `sj-lab-openapi` | `db/openapi_api_key.sql`, `db/openapi_api_usage.sql`(신규), `key/`(Repository·Service·Controller·Record), `auth/AuthClient`, `config/ApiKeyProperties`·`ApiKeyStoreConfig`, 중계에 키 검사·사용량 기록 연결, 테스트 4건, README·CLAUDE.md |
| `sj-lab-openapi-web` | `components/KeyPanel.js`(신규), `api.js`(키 API·헤더 전송), `ApiDetail`·`CodeSamples`에 키 반영, README·CLAUDE.md |
| `mapservice-rest` | `docs/system-architecture.md`(키·사용량 설명) |

## 사용자 확인이 필요한 것

1. **표 만들기** — 개발 DB(`sjlab`)에 위 두 스크립트 실행. 지시하시면 제가 실행하고 결과를 기록하겠습니다.
2. **DB 계정** — 이 서비스가 쓸 계정(INSERT 권한 필요). 로컬은 `.claude/settings.local.json`, 운영은 k8s Secret에 둡니다.

## 남은 단계

4. 허브 4번째 카드 열기 · 게이트웨이 `/open-api` 라우트 · Helm 차트 · nginx 경로 · 배포
