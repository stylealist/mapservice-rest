# history v1.14 — 시설물 팝업을 열 때 화면이 위로 밀리던 오류 수정

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.13.md](history_v1.13.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 지도 핀 클릭으로 재현·확인 |

## 실행한 프롬프트

```
시설물을 클릭해서 팝업을 띄울떄 <main class="main-content loaded" style="transform: translateY(0px); height: 100vh; min-height: 100vh;">영역이 위로 올라가서 상단부분이 잘려보이는 오류가 있어 해결해줘
```

## 원인 (재현으로 확인)

지도에서 핀을 클릭했을 때의 측정값입니다.

| 값 | 클릭 전 | 클릭 후(수정 전) |
|---|---|---|
| `main` 화면상 위치 | 60px | **38px** (22px 밀림) |
| 목록 스크롤 위치 | 1,651 | 10,993 |
| 팝업 상단 위치 | - | **-6px** (화면 위로 삐져나감) |

원인은 두 가지였습니다.

1. **`scrollIntoView`가 상위 문서까지 스크롤** — 선택된 목록 항목을 보이게 하려고 `item.scrollIntoView()`를 썼는데, 이 API는 목록 컨테이너뿐 아니라 스크롤 가능한 **모든 조상**을 함께 스크롤합니다. 그래서 화면 전체가 위로 밀려 헤더 아래가 잘렸습니다.
2. **팝업 위치 보정이 동작하지 않음** — 내용을 그린 뒤 같은 좌표로 `setPosition()`을 다시 호출했지만, 값이 바뀌지 않으면 OpenLayers가 변경 이벤트를 발생시키지 않아 `autoPan`이 아예 실행되지 않았습니다. 게다가 지도 이동 애니메이션이 끝나기 전에 보정하면 애니메이션이 팝업을 다시 화면 밖으로 밀어냅니다.

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-facility.js` | `scrollFacilityItemIntoView()` 신설(목록 컨테이너만 스크롤), `ensureFacilityPopupVisible()` 신설(애니메이션 종료 후 `panIntoView`) |

- **목록 스크롤**: 컨테이너와 항목의 위치를 비교해 **필요한 만큼만** 컨테이너를 스크롤합니다. 문서는 건드리지 않습니다.
- **팝업 보정**: `setPosition` 재호출 대신 `panIntoView({ margin: 24 })`를 쓰고, `view.getAnimating()`이 끝날 때까지(최대 1.5초) 기다린 뒤 한 번만 실행합니다.

## 검증 결과

| 확인 | 수정 전 | 수정 후 |
|---|---|---|
| 목록 항목 클릭 후 `main` 위치 | 60px (정상) | 60px |
| **지도 핀 클릭 후 `main` 위치** | **38px (밀림)** | **60px (정상)** |
| 팝업 상단 위치 | -6px (잘림) | **84px (온전히 보임)** |
| 선택 항목이 목록에 보이는지 | 보임 | 보임 |
| 콘솔 오류 | 0건 | 0건 |

## 참고

- 목록 스크롤 위치(11,001)는 선택된 항목이 목록 아래쪽에 있어 컨테이너가 그만큼 내려간 정상 동작입니다.
- 지도 프론트엔드는 아직 실서버 `/map` 경로에서 서비스되지 않는 상태입니다(v1.10 확인).
