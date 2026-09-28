# history v2.42 — 왼쪽 패널 스크롤 두 줄 제거, 목록 아래가 잘리던 문제 수정

- **날짜**: 2026-09-28
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.41.md](history_v2.41.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 왼쪽 시설물 목록 |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 후 확인 |

## 실행한 프롬프트

```
1. panel-content 안에 스크롤이 2곳이 나와 .facility-list-container 의 스크롤만 나오도록 해줘
2. 스크롤을 가장 아래로 내려도 시설물들이 잘려서 나오지 않는 오류가 있어
```

## 원인

둘 다 높이를 화면 기준 `calc()`로 잡아 둔 데서 나왔습니다.

| 요소 | 전 | 문제 |
|---|---|---|
| `.layer-panel` | `height: 100vh` | 지도 컨테이너가 헤더(60px) 아래에서 시작하면서 높이는 `100vh`라 **패널이 화면 아래로 60px 넘쳐** 있었습니다. 목록 마지막 항목이 화면 밖에 그려져 끝까지 스크롤해도 잘려 보였습니다(②의 원인). |
| `.panel-content` | `height / max-height: calc(100vh - 60px)` + `overflow-y: auto` | 목록 컨테이너와 **스크롤이 두 겹**(①의 원인). |
| `.facility-list-container` | `max-height: calc(100vh - 320px)` | 위쪽 검색·필터 영역 높이가 바뀌면(내업 상태 필터가 나타나는 등) 320이라는 값이 어긋나 아래가 잘렸습니다. |

## 작업 내용 (`css/components/layer-panel.css`)

패널을 **세로 플렉스**로 바꿔 높이를 물려주고, 스크롤은 목록 한 곳만 남겼습니다.

| 요소 | 후 |
|---|---|
| `.layer-panel` | `height: calc(100vh - 60px)` (화면에 보이는 아래 끝까지만) + `display: flex; flex-direction: column` |
| `.panel-content` | `flex: 1 1 auto; min-height: 0; overflow: hidden; display: flex; flex-direction: column` — **스크롤 없음** |
| `.tab-content` | 같은 방식으로 남은 높이 전달 |
| `#facility-tab.tab-pane.active` | `display: flex; flex-direction: column; overflow: hidden` (검색·필터는 고정) |
| `.tab-pane.active`(그 밖의 탭) | `overflow-y: auto` — 패널에 스크롤이 없으므로 탭이 스스로 스크롤 |
| `.facility-search-area` | `flex: 0 0 auto` |
| `.facility-list-container` | `flex: 1 1 auto; min-height: 0; overflow-y: auto` + 아래 여백 0.4rem — **여기서만 스크롤** |

`index.html`의 참조를 `layer-panel.css?v=20260928`로 올렸습니다.

## 검증 (로컬, 헤드리스 Chrome + CDP)

패널 안에서 **실제로 스크롤이 생기는 요소**를 전부 찾아 확인했습니다(`overflow-y`가 auto/scroll이면서 내용이 넘치는 것).

| 화면 | 스크롤 요소 | 패널 위치 | 목록 끝까지 내렸을 때 |
|---|---|---|---|
| 1440×1000 | `facility-list-container` **하나** | 60 ~ 1000 (화면 아래 잘림 없음) | 마지막 항목 `914~975` — 화면 안·목록 안 모두 ✔ |
| 1280×800 | `facility-list-container` **하나** | 60 ~ 800 | 마지막 항목 `714~775` ✔ |

시설물 248건 기준으로 확인했고 콘솔 오류는 없습니다.

## 문서

- `docs/ui-conventions.md` — "왼쪽 패널 스크롤은 시설물 목록 한 곳에서만" 규칙 추가(위쪽에 `overflow-y`를 되살리지 말 것, 목록 높이를 `calc(100vh - ...)`로 잡지 말 것, 패널 높이는 `calc(100vh - 60px)`인 이유, 다른 탭은 스스로 스크롤).

## 남은 작업

- 커밋·push.
- 운영 배포 후 실제 브라우저에서 목록 끝까지 스크롤되는지 확인.
