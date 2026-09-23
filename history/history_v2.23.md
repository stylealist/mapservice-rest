# history v2.23 — 헤더 로고 클릭 시 404 나던 문제 수정

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.22.md](history_v2.22.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 헤더 로고 클릭 |
| 운영 지도 | https://sj-lab.co.kr/map/ | |

## 실행한 프롬프트

```
logoHome 를 클릭했을때 404에러가 나고있어
```

## 원인

`js/modules/ui.js`의 `goToMap()`이 **절대 경로로 통째 이동**하고 있었습니다.

```js
const homeUrl = window.location.origin + "/map";   // ← 운영 기준으로 하드코딩
window.location.href = homeUrl;
```

- 운영(`sj-lab.co.kr/map/`)에서는 우연히 맞았지만,
- **로컬은 루트(`localhost:4000`)에서 서빙**하므로 `/map` 경로가 없어 **404**.
- 게다가 새로고침이라 지도 상태(배율·선택·필터)도 함께 날아갔습니다.

## 조치

| 항목 | 파일 | 내용 |
|---|---|---|
| 로고 동작 | `js/modules/ui.js` | `goToMap()`을 **SPA 페이지 전환**으로 변경 — `.nav-btn[data-page="map"]`을 클릭해 `initializeNavigation`이 바인딩한 전환 핸들러를 그대로 탄다. 주소는 그대로고 지도도 다시 만들지 않는다. 버튼을 못 찾으면 `./`(현재 디렉터리 첫 화면)로 폴백 |
| 문서 | `docs/ui-conventions.md` | "로고 클릭은 SPA 전환으로 처리하고 **절대 경로로 이동시키지 말 것**"을 이유(운영에만 있는 경로)와 함께 규칙화 |

허브 링크(`#hubLink`, v2.22)만 운영/로컬 분기를 갖습니다 — 그쪽은 실제로 **다른 사이트**로 가는 링크이기 때문입니다.

## 검증 (로컬, 헤드리스 Chrome + CDP)

| 단계 | 결과 |
|---|---|
| 시작 | `active = map-page`, URL `http://localhost:4000/` |
| 소개 탭 클릭 | `active = about-page`, URL 그대로 |
| **로고 클릭** | `active = map-page`, URL 그대로, **지도 캔버스 유지**(404 없음) |

## 남은 작업

- 커밋·push 후 운영에서도 로고 클릭 확인(운영은 기존에도 동작했지만 이제 새로고침이 사라짐).
