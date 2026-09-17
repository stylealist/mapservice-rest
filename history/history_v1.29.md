# history v1.29 — 시설물 상세 팝업을 지도 이동이 끝난 뒤 최종 위치에서 한 번에 표시

- **날짜**: 2026-09-17
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.28.md](history_v1.28.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 프론트엔드 | http://localhost:4000 | 지도에서 시설물 핀 클릭 / 좌측 목록에서 항목 클릭 (변경 반영은 강력 새로고침) |

## 실행한 프롬프트

```
서버 기동해줘
```

(로컬 스택 기동만 한 턴이라 파일 변경 없음)

```
지도가 가운데로 이동하긴하는데 상세팝업이 이동전에 아래쪽에 나왔다가 이동후에 위치가 변경되서 정신이없어 그냥 이동이 끝난후 해당 위치에 맞게 나오도록 해줄 수 있어?
```

## 원인

시설물을 고르면 세 가지가 서로 다른 시점에 일어나 팝업이 두 번 움직였습니다.

1. `showFacilityDetail()`이 **바로 `setPosition()`** — 팝업이 `top-left` 기준이라 핀 **아래쪽**에 "불러오는 중" 상태로 먼저 보임
2. 동시에 `selectFacility()`의 **가운데 이동 애니메이션(400ms)** 과 오버레이 **`autoPan`(250ms)** 이 겹쳐 실행
3. 상세 응답이 오고 이동이 끝난 뒤 `ensureFacilityPopupVisible()`이 **높이 기준 세로 가운데 오프셋**을 적용 → 팝업이 위로 점프, 필요하면 `panIntoView`로 한 번 더 이동

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-facility.js` | 오버레이 `autoPan` 끔, `ensureFacilityPopupVisible()` → `revealFacilityPopup(token)` + `whenFacilityViewSettled()`로 교체, 표시 순번(`facilityPopupRevealToken`)·느린 응답 대비(`FACILITY_POPUP_SLOW_REVEAL_MS`) 추가 |
| `css/components/layer-panel.css` | `.facility-popup.is-positioning { visibility: hidden; }` 추가 |
| `docs/map-architecture.md` | "팝업 위치" 항목을 새 표시 순서·주의사항으로 갱신 (기존 `autoPan` 켜기 설명 대체) |

- **표시 순서**: 팝업을 `is-positioning`(숨김)으로 자리만 잡음 → 가운데 이동 끝 → 높이 기준 세로 가운데 오프셋 → 화면 밖이면 `panIntoView` → 그 보정 이동까지 끝나면 숨김 해제.
- **`display: none`이 아니라 `visibility: hidden`** 으로 숨깁니다. 레이아웃이 유지돼야 높이를 재서 위치를 계산할 수 있습니다.
- **`autoPan`을 끈 이유**: `setPosition` 직후 실행돼 가운데 이동 애니메이션과 겹치던 것이 흔들림의 한 원인이었습니다. 화면 안 보정은 이동이 끝난 뒤 `panIntoView`로 직접 합니다.
- **다른 시설물을 연달아 고르거나 닫으면** 표시 순번을 올려 이전 선택의 대기 중 표시를 취소합니다.
- **상세 응답이 1.2초보다 늦으면** "불러오는 중" 상태로라도 먼저 보여주고, 내용이 채워지면 위치만 다시 맞춥니다(아무것도 안 보이는 시간이 길어지지 않도록).
- 오류 응답일 때도 `finally`에서 같은 방식으로 오류 문구를 최종 위치에 표시합니다.

## 검증 결과 (실제 Chrome 1400×950, 매 프레임 기록)

클릭 후 3초 동안 `requestAnimationFrame`마다 팝업 표시 여부·위치와 지도 애니메이션 여부를 기록했습니다.

| 경로 | 처음 보인 시각 | 보일 때 지도 이동 중 | 보인 뒤 이동 중 프레임 | 보인 뒤 위치 변화 |
|---|---|---|---|---|
| 지도 핀 클릭 | 471ms | 아니오 | 0 | **0px, 0px** |
| 목록 클릭(줌 16) | 444ms | 아니오 | 0 | **0px, 0px** |
| 연속 선택(120ms 간격 두 번) | 580ms | 아니오 | 0 | **0px, 0px** |
| 선택 직후 닫기 | - | - | - | 늦게 뜨지 않음(정상) |

콘솔 오류 0건. 이동 시간(400ms) 동안은 팝업이 보이지 않고, 끝난 직후 최종 위치에 한 번만 나타납니다.

## 참고

- 사진 갤러리 이미지가 표시 후에 늦게 로드돼 팝업 높이가 바뀌는 경우의 재정렬은 이번 범위에 넣지 않았습니다(기존 동작과 같음).
- 커밋·push는 하지 않았습니다.
