# history v2.39 — 지도 범례 추가 (시설물은 항상, 그 밖의 레이어는 켠 것만)

- **날짜**: 2026-09-28
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.38.md](history_v2.38.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 왼쪽 아래 "범례" |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 후 확인 |

## 실행한 프롬프트

```
현재 범례가 없어서 범례가 나왔으면 좋겠어 시설물의 경우는 on/off가 없기때문에 표시되어야하고
다른 레이어들의 경우에는 on되었을때만 범례에 나오도록 해줘
```

## 작업 내용

### 1. 범례 모듈 (`js/modules/map/map-legend.js` 신규)

지도 왼쪽 아래에 지금 보이는 기호를 설명하는 상자를 추가했습니다. 구성은 **시설물 상태 → 켜 둔 레이어 → 시설물 종류** 순입니다(방금 켠 레이어가 스크롤 없이 보이도록).

| 섹션 | 내용 | 표시 조건 |
|---|---|---|
| 시설물 상태 | 보수 불필요(파랑) · 보수 필요(주황) · 보수 필요 · 내업 완료(초록) · 겹친 핀 묶음(개수) | **항상** (묶음 항목만 "핀 묶어 보기"가 켜져 있을 때) |
| 켜 둔 레이어 | 편의점 · 버스정류장 · CCTV · 약국 · 병원 · 관공서(WFS), 편의점(WMS) | 그 레이어가 **켜져 있을 때만** |
| 시설물 종류 | 주차장 · 전기차 충전소 · 강당·강의실 · 구내식당·카페 · 체육시설 · 전시시설 · 시설물(기본) | **항상** (DB `map.facility_icon` 설정 순서) |

- **아이콘을 새로 만들지 않았습니다.** 지도에 쓰는 함수에서 그대로 가져옵니다 — 시설물은 `buildFacilityIconUrl()`·`buildFacilityClusterIconUrl()`, WFS는 스타일의 `ol.style.Icon#getSrc()`. 그래서 아이콘·색 규칙(예: DB 아이콘 추가)을 바꾸면 범례도 자동으로 따라갑니다.
- 각 레이어 모듈에 목록 제공 함수를 하나씩 추가했습니다: `getFacilityLegendItems()`(map-facility.js), `getWfsLegendItems()`(map-wfs.js), `getWmsLegendItems()`(map-wms.js).
- 갱신 시점: 지도 레이어 컬렉션 전체의 `change:visible`(이후 추가되는 레이어까지 구독) + `sjlab:facility-legend-changed` 커스텀 이벤트(시설물 아이콘 DB 설정 로드, 핀 묶어 보기 토글). 50ms 안에 여러 번 불려도 한 번만 그립니다.
- 머리글을 누르면 접히고, 그 상태를 `localStorage`(`sjLabMapLegendOpen`)에 기억합니다.

### 2. 위치 — 오른쪽 아래에서 **왼쪽 아래**로

처음에는 관례대로 오른쪽 아래에 두었는데 두 가지 문제가 있었습니다.

| 문제 | 원인 | 처리 |
|---|---|---|
| 아래가 잘림 | `.main-content`가 헤더 높이만큼 화면 아래로 넘쳐 있어 컨테이너 기준 `bottom`이 화면 밖 | `positionLegendAboveViewportBottom()`이 넘친 만큼 재서 더함(창 크기 변경·다시 그릴 때마다) |
| 오른쪽 세로 컨트롤(측정·로드뷰·편의시설·교통·지도캡쳐)을 덮음 | 1280×800·1366×700에서 컨트롤 줄이 화면 아래까지 내려옴 | 높이를 깎아 봤지만 낮은 화면에서는 부족 → **왼쪽 아래(레이어 패널 오른쪽, `left: 348px`)로 이동** |

### 3. 파일

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-legend.js` | 신규 — 범례 렌더링·구독·접기·위치 보정 |
| `js/modules/map/map-facility.js` | `getFacilityLegendItems()` 추가, 아이콘 설정 로드·묶어 보기 토글 시 `sjlab:facility-legend-changed` 발생 |
| `js/modules/map/map-wfs.js` | `getWfsLegendItems()` 추가 |
| `js/modules/map/map-wms.js` | `getWmsLegendItems()` 추가 |
| `js/modules/map/map.js` | 범례 import·초기화(`initializeMapWithModules` 안, 시설물 다음)·`window` 등록 |
| `index.html` | `#mapLegend` 마크업, `map-legend.css?v=20260928` 링크 |
| `css/components/map-legend.css` | 신규 — 위치·접기·아이콘 목록(종류는 2열)·좁은 화면 대응 |
| `docs/map-architecture.md`, `README.md` | 모듈 설명과 "반드시 지킬 것"(아이콘을 범례에서 새로 만들지 말 것, 오른쪽으로 옮기지 말 것 등) |

## 검증 (로컬, 헤드리스 Chrome + CDP)

| 확인 | 결과 |
|---|---|
| 접속 직후 | 시설물 상태 4개(묶음 포함) + 시설물 종류 7개 표시, "켜 둔 레이어" 섹션 없음 |
| 편의점 켜기 | "켜 둔 레이어: 편의점" 추가 |
| CCTV 추가 | "켜 둔 레이어: 편의점, CCTV" |
| 편의점 끄기 | "켜 둔 레이어: CCTV" (시설물 섹션은 그대로) |
| 핀 묶어 보기 끄기 | 시설물 상태에서 "겹친 핀 묶음(개수)"만 사라짐 |
| 접기 → 펼치기 | 접힘 상태·`localStorage` 저장값(`off`) 정상, 다시 펼쳐짐 |
| 1280×800 · 1366×700 | 화면 아래로 잘리지 않음, 오른쪽 컨트롤·왼쪽 패널과 겹치지 않음 |
| 소개 탭 | 지도 페이지가 숨겨지므로 범례도 보이지 않음(`offsetParent` 없음) |
| 아이콘 | 모든 `<img>`가 실제로 로드됨(naturalWidth > 0), 콘솔 오류 0건 |

검증 중 헤드리스 브라우저가 **예전 모듈을 캐시해** 범례가 비어 보이는 일이 있었습니다(파이썬 서버 시절 캐시). 테스트 스크립트에 `Network.setCacheDisabled`를 넣어 해결했습니다 — 코드 문제가 아니었습니다.

## 남은 작업

- 커밋·push 여부 확인.
- 운영 배포 후 실제 브라우저에서 범례 표시 확인(모듈 캐시가 남아 있으면 새로고침 필요).
