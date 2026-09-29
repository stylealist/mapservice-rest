# history v2.55 — API 활용 페이지 2단계: 화면 만들기

- **날짜**: 2026-09-29
- **영향 저장소**: `sj-lab-openapi-web`(신규 구현), `mapservice-rest`(문서·기록)
- **이전 버전**: [history_v2.54.md](history_v2.54.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 작업 로그 |
| 활용 페이지(로컬) | http://localhost:4100 | `npm start` |
| 허브(로컬) | http://localhost:3000 | 이번에 함께 기동 |
| 저장소 | https://github.com/stylealist/sj-lab-openapi-web | React + Webpack |

## 실행한 프롬프트

```
sj-lab-hub 기동시켜주고 바로 2단계 진행해줘
```

## 만든 화면

API를 **보고 → 그 자리에서 눌러 보고 → 코드로 복사해 가는** 한 페이지입니다.

| 영역 | 내용 |
|---|---|
| 왼쪽 | API 목록 12개(공공데이터 6 · 시설물 3 · 행정구역 3), 묶음별 설명 |
| 오른쪽 위 | 고른 API 설명 + **실제 호출 주소**(복사 버튼) |
| 파라미터 | 이름 · 필수 여부 · 설명 · 입력칸. 예시값이 미리 채워져 있어 바로 눌러볼 수 있음 |
| 실행해 보기 | 호출 결과를 **상태 · 걸린 시간 · 크기 · 본문**으로 표시 |
| 샘플 코드 | curl / JavaScript / Python — 지금 입력한 값이 들어간 코드를 복사 |

**화면을 코드에 적지 않았습니다.** 서버가 내려주는 API 목록(`/open-api/catalog`)을 읽어 그립니다.
그래서 나중에 API가 늘어도 이 페이지는 고칠 필요가 없습니다.

### 두 가지 주소

| 쓰임 | 로컬 | 운영 |
|---|---|---|
| 실제 호출 | 빈 주소(웹팩 개발 서버가 게이트웨이로 넘겨줌) | `https://api.sj-lab.co.kr/open-api` |
| 화면·샘플 코드에 보여 줄 주소 | `http://localhost:8100/open-api` | 같음 |

로컬에서 개발 서버가 대신 호출해 주므로 **CORS 설정을 새로 만들지 않았습니다**. 운영은 `sj-lab.co.kr`에서
`api.sj-lab.co.kr`를 부르는데, 게이트웨이가 이미 허용한 주소입니다.

로그인은 다른 sj-lab 사이트와 같은 화면을 씁니다(허브·지도와 같은 스크립트).

## 검증 (헤드리스 Chrome, 1440×900)

| 확인 | 결과 |
|---|---|
| API 목록 | 12개 · 묶음 3개 표시 |
| 첫 화면 | "편의점" 자동 선택, 주소·파라미터 2개 표시 |
| 실행해 보기(편의점) | **HTTP 200** · 684 ms · 197.7 KB · `application/json` |
| 없는 시설물 ID | **HTTP 404**를 화면에 그대로 표시 |
| 샘플 코드 | curl / JavaScript / Python 전환·복사 동작 |
| 콘솔 오류 | **없음** |
| `npm run build` | 성공 (bundle 161 KiB) |

처음엔 React 경고가 2건 떴습니다 — 선택 상태 스타일에서 `border`와 `borderColor`를 섞어 써서였고,
`border` 한 줄로 덮어쓰도록 고쳐 없앴습니다. 주소의 `%2C`도 쉼표로 되돌려 읽기 쉽게 했습니다.

## 바뀐 파일

| 저장소 | 내용 |
|---|---|
| `sj-lab-openapi-web` | React 앱 전체 — `App.js`(헤더·목록·배치), `components/ApiDetail.js`, `components/CodeSamples.js`, `api.js`, `public/index.html`(로그인 게이트), `favicon.svg`, webpack·babel 설정, README, CLAUDE.md |
| `mapservice-rest` | `docs/dev-environment.md`(4100 실행 방법·프록시), `docs/system-architecture.md`(활용 페이지 설명) |

## 남은 단계

3. API 키 발급·사용량 (DB 스크립트는 만들고 실행은 담당자)
4. 허브 4번째 카드 열기 · 게이트웨이 `/open-api` 라우트 · 차트 · nginx 경로 · 배포
