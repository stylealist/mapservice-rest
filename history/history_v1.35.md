# history v1.35 — 편의점·약국·병원·관공서·버스·CCTV 레이어 팝업도 헤더 드래그로 이동

- **날짜**: 2026-09-17
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.34.md](history_v1.34.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 프론트엔드 | http://localhost:4000 | 우측 편의시설·교통 버튼으로 레이어를 켜고 아이콘 클릭 → 팝업 헤더를 끌기 (반영은 Ctrl+F5) |

## 실행한 프롬프트

```
편의점, 약국, 병원, 관공서, 버스, cctv 등 레이어의 상세 팝업도 드래그 앤 드랍으로 상세창을 이동 시킬 수 있도록 해줘
```

## 원인

헤더 드래그 이동(v1.24)은 시설물 팝업 전용 함수 `bindFacilityPopupDrag()`로만 구현되어 있었습니다.
WFS 레이어 6종은 `map-wfs.js`의 `showWfsPopup()` 하나를, WMS 레이어는 `map-wms.js`의 `showWmsPopup()`을 쓰는데 두 곳에는 드래그가 없었습니다.

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-popup-drag.js` | **신규** — 공용 `bindOverlayHeaderDrag(overlay, header, { ignoreSelector })`. 헤더에 이동 커서·`touch-action: none`을 지정하고, 끄는 동안 오버레이 offset만 변경 |
| `js/modules/map/map-wfs.js` | `showWfsPopup()`에서 오버레이 생성 직후 헤더 드래그 연결(닫기 버튼 제외) — 편의점·약국·병원·관공서·버스·CCTV 공통 |
| `js/modules/map/map-wms.js` | `showWmsPopup()`에도 같은 방식으로 연결 |
| `js/modules/map/map-facility.js` | 전용 `bindFacilityPopupDrag()` 삭제 → 공용 함수 사용(동작 동일) |
| `docs/map-architecture.md` | 모듈 목록에 `map-popup-drag.js`와 사용 규칙 추가 |

- 오버레이 **좌표는 대상 지점에 고정하고 offset만** 바꾸므로, 옮긴 뒤 지도를 움직여도 팝업이 그 지점을 따라갑니다(v1.24 시설물 팝업과 같은 방식).
- WFS·WMS 팝업은 클릭할 때마다 새로 만들어지므로 드래그로 옮긴 위치는 다음 팝업에 이어지지 않습니다.
- 요청 목록에는 없었지만 WMS 팝업도 구조가 같아 함께 적용했습니다.

## 검증 결과 (실제 Chrome 1400×950)

각 팝업의 헤더를 (−140, +70) 끌고, 지도를 100px 옮긴 뒤, 닫기 버튼을 눌렀습니다.

| 팝업 | 여는 방법 | 헤더 커서 | 드래그 이동 | 지도 이동 후 추종 | 닫기 버튼 |
|---|---|---|---|---|---|
| 편의점 | `showWfsPopup` 직접 호출※ | move | −140, +70 | −100, 0 | 닫힘 |
| 약국 | 지도 아이콘 클릭 | move | −140, +70 | −100, 0 | 닫힘 |
| 병원 | 지도 아이콘 클릭 | move | −140, +70 | −100, 0 | 닫힘 |
| 관공서 | 지도 아이콘 클릭 | move | −140, +70 | −100, 0 | 닫힘 |
| 버스정류장 | 지도 아이콘 클릭 | move | −140, +70 | −100, 0 | 닫힘 |
| CCTV | `showWfsPopup` 직접 호출※ | move | −140, +70 | −100, 0 | 닫힘 |
| WMS | `showWmsPopup` 직접 호출 | move | −140, +70 | −100, 0 | 닫힘 |
| 시설물 | 목록 클릭 | move | −140, +70 | −100, 0 | 닫힘 |

※ 편의점·CCTV는 검증 스크립트의 지도 클릭 좌표가 아이콘을 빗맞아 팝업이 열리지 않아, 클릭 시 호출되는 같은 함수(`showWfsPopup`)를 실제 피처로 직접 불러 확인했습니다. 클릭 처리 코드는 이번에 바꾸지 않았습니다.

- 페이지 오류 없음. 콘솔 오류 2건은 CCTV 외부 스트리밍 서버(`cctvsec.ktict.co.kr`)가 401을 돌려준 것으로, 이번 변경과 무관합니다.
- 검증 중 스크립트 문제 두 가지(레이어를 켤 때 데이터 로드를 하지 않는 `toggleWfsLayer`를 부른 것, 팝업이 우측 도구·상단 헤더에 가려 닫기 클릭이 막힌 것)를 고쳐 다시 측정했습니다.

## 참고


## 커밋·푸시

후속 요청 `커밋하고 푸시해줘`에 따라 v1.33~v1.35 변경을 커밋하고 `origin/main`에 푸시했습니다.

| 저장소 | 커밋 | 내용 |
|---|---|---|
| `sj-lab-mapservice` | `8fb60bb` | 보수 필요 여부 필터(v1.33) · 팝업 보수 불필요 표시(v1.34) · 레이어 팝업 드래그 이동(v1.35). `map-facility.js`·`layer-panel.css`·문서가 세 작업에 걸쳐 있어 한 커밋으로 묶음 |
| `mapservice-rest` | 이 문서를 포함한 커밋 | history v1.33~v1.35 + 공유 페이지 |
