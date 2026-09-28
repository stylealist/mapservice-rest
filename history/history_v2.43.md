# history v2.43 — 배경지도 반복 클릭 시 하이브리드가 껌뻑이던 버그 수정, 소개 탭 기능·기술 보강

- **날짜**: 2026-09-28
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.42.md](history_v2.42.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 우측 상단 일반/위성 버튼 |
| 로컬 소개 탭 | http://localhost:4000 → 상단 "소개" | 기능·기술 목록 |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 후 확인 |

## 실행한 프롬프트

```
1. 일반지도, 위성지도를 각각 같은곳을 여러번 누를경우 하이브리드가 켜졌다 꺼졌다가 하는 오류수정해줘
2. 소개에서 이런 기능들이 있습니다, 사용한 기술 부분에 내가 사용한 기술들이 안적혀있는부분들이 있는거같아 모두 나오도록 해줘
```

## 1. 하이브리드 오버레이 버그

**원인**: `ui.js`의 배경지도 버튼 핸들러가 `toggleOverlay("hybrid")`로 **상태를 뒤집고** 있었습니다. 지도 종류와 상관없이 누를 때마다 반대로 바뀌므로, 위성을 두 번 누르면 도로·지명이 사라지고 일반 지도를 두 번 누르면 일반 지도 위에 하이브리드가 얹혔습니다.

```javascript
// 전 — 어느 버튼이든 누를 때마다 뒤집힘
if (mapType === "satellite") toggleOverlay("hybrid");
else if (mapType === "common") toggleOverlay("hybrid");

// 후 — 위성일 때만 켜지도록 상태를 정해 준다
window.setOverlayVisible("hybrid", mapType === "satellite");
```

- `map-layers.js`에 **`setOverlayVisible(overlayType, visible)`** 를 추가하고(`map.js`에서 `window`에 등록), 배경지도 버튼은 이 함수를 씁니다. 몇 번을 눌러도 결과가 같습니다.
- 기존 `toggleOverlay()`는 그대로 두되, "상태가 정해져 있는 자리에는 쓰지 말 것"이라는 주석과 문서 규칙을 남겼습니다.

**검증** (헤드리스 Chrome, 레이어 가시성 직접 확인)

| 조작 | 일반 | 위성 | 하이브리드 |
|---|---|---|---|
| 초기 | ✔ | - | - |
| 위성 1·2·3회 연속 | - | ✔ | **✔ (계속 켜짐)** |
| 일반 1·2회 연속 | ✔ | - | **- (계속 꺼짐)** |
| 다시 위성 | - | ✔ | ✔ |

## 2. 소개 탭 — 기능·기술 모두 표기

실제 코드에서 쓰는 것을 확인해 빠져 있던 항목을 채웠습니다.

**기능 카드 6개 → 11개** (추가한 5개)

| 카드 | 근거 |
|---|---|
| 공공데이터 레이어 6종 | `map-wfs.js`(편의점·버스정류장·CCTV·약국·병원·관공서, bbox 조회·줌별 표본), `map-wms.js`(GeoServer) |
| 로드뷰로 현장 둘러보기 | `map-roadview.js` + 카카오맵 SDK, `html/loadview/load-view.html` |
| 지도 캡처 후 표시 · 편집 | `map-area-selector.js` + `html/fabric/fabric-editor.html`(Fabric.js 5.3) |
| 지금 보이는 기호를 설명하는 범례 | `map-legend.js`(v2.39~v2.41) |
| 현장 계정 그대로 쓰는 로그인 | `auth-gate.js` + `sj-lab-authserver`(QFieldCloud 계정 위임, 공유 로그인 화면) |

**기술 스택 3그룹 → 4그룹 · 뱃지 35개**

| 그룹 | 내용 |
|---|---|
| 프론트엔드 · 지도 | OpenLayers, 바닐라 JavaScript, ES 모듈(빌드 도구 없음), GeoJSON·WFS, VWorld 지도 API, 카카오맵 로드뷰 SDK, hls.js, Fabric.js, Canvas(사진 리사이즈) |
| 백엔드 · 데이터 | Spring Boot 3.3, Java 17, MyBatis, PostgreSQL 17, PostGIS 3.4, OpenPDF, springdoc OpenAPI, GeoServer(WMS) |
| 현장 수집 · 동기화 | QField 포크(C++·QML), QGIS, QFieldCloud, GeoPackage(GPKG), Python 워커, GeoPandas·Shapely, psycopg2·SQLAlchemy, FastAPI |
| 인프라 · 배포 | Kubernetes(k3s), Helm, ArgoCD, Jenkins, Docker, NCP 컨테이너 레지스트리, nginx, Spring Cloud Gateway, Eureka, JWT 로그인 서버 |

- 그룹이 4개가 되면서 `.tech-groups`를 `repeat(auto-fit, minmax(280px, 1fr))`로 바꿔 폭에 맞게 배치되도록 했습니다(3열 고정이면 4번째가 혼자 남음).
- 추가한 항목은 모두 저장소에서 사용을 확인한 것만 넣었습니다(`pom.xml`의 springdoc·OpenPDF, `qfield_data_sync.py`의 GeoPandas·psycopg2·SQLAlchemy, `fabric-editor.html`의 Fabric.js 5.3.0, `load-view.html`의 카카오 SDK 등).

## 검증 (로컬, 헤드리스 Chrome + CDP, 1440×1000)

| 확인 | 결과 |
|---|---|
| 배경지도 반복 클릭 | 위 표대로, 연속 클릭에도 상태 유지 |
| 소개 탭 | 기능 카드 11개·기술 그룹 4개·뱃지 35개 표출, 가로 스크롤 없음 |
| 콘솔 오류 | 없음 |

## 문서

- `docs/map-architecture.md` — `map-layers.js` 설명에 `setOverlayVisible` 추가, "상태가 정해진 자리에 `toggleOverlay`를 쓰지 말 것" 규칙 기록.
- `README.md` — 기술 스택을 6줄(프론트·지도·연동 라이브러리·백엔드·현장 수집·인프라)로 다시 쓰고, 현장 확인 보조 도구(공공데이터 레이어·로드뷰·측정·캡처 편집) 항목 추가.
- `index.html` — `info-pages.css?v=20260928b`.

## 남은 작업

- 커밋·push (v2.42 패널 스크롤 수정과 함께).
