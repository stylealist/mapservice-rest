# history v2.15 — 시설물 핀 묶음(클러스터링) 추가

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.14.md](history_v2.14.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 시설물 탭 → "핀 묶어 보기" |
| 로컬 로그인 페이지 | http://localhost:8100/auth/login.html | 로그인 게이트 통과용 |
| 운영 지도 | https://sj-lab.co.kr/map/ | |

## 실행한 프롬프트

```
다시 추가 리스트 알려줘
클러스티링 진행해줘
```

## 작업 내용

전국 조회 시 2,400여 개의 핀이 겹쳐 개수를 가늠할 수 없던 문제를 해결하기 위해, 가까운 핀을 하나로 묶어 개수를 보여주고 누르면 그 범위로 확대하도록 했습니다. **백엔드·API 변경은 없습니다.**

| 항목 | 파일 | 내용 |
|---|---|---|
| 묶음 소스 | `js/modules/map/map-facility.js` | `ol.source.Cluster`(`facilityClusterSource`)를 원본 소스 위에 씌우고 레이어의 소스·스타일만 교체(`applyFacilityClustering()`). **원본 `facilitySource`는 그대로** 두어 `getFeatureById()`·목록 연동·데이터 적재 경로는 손대지 않음 |
| 필터 연동 | 〃 | 필터 판정을 `geometryFunction`(`facilityClusterGeometry()`)에서 수행 — 탈락 피처는 묶음에서 제외되므로 **배지 숫자가 목록 건수와 항상 일치**. 스타일에서만 거르면 숫자가 어긋남 |
| 묶음 스타일 | 〃 | 1건이면 기존 핀 그대로, 2건 이상이면 개수 배지. 색은 핀 규칙을 따름(처리할 보수 건 있으면 주황 `#d97706`, 전부 내업 완료면 초록 `#059669`, 그 외 파랑 `#2563eb`). 반지름은 로그 스케일(14~26px). 선택한 시설물이 묶음 안이면 강조 링. 조합은 `facilityClusterStyleCache`에 캐시 |
| 묶음 클릭 | 〃 | 선택이 아니라 **범위 확대**(`expandFacilityCluster()`). 좌표가 사실상 같은 묶음은 `fit` 시 최대 배율로 튀므로 2단계씩만 확대 |
| 다시 그리기 | 〃 | 필터·선택 변경 시 `facilitySource.changed()` → **`refreshFacilityLayer()`**로 교체(묶음은 개수까지 재계산해야 함). 호출부 5곳 수정 |
| 토글 UI | `index.html`, `css/components/layer-panel.css` | 내업 필터 줄 아래 체크박스 `#facilityClusterToggle`("핀 묶어 보기"). 선택은 `localStorage`(`sjLabFacilityCluster`)에 기억(읽기·쓰기 try/catch) |
| 캐시 무효화 | `index.html` | 수정한 `layer-panel.css` 참조에 `?v=20260923` |
| 문서 | `docs/map-architecture.md`, `docs/ui-conventions.md` | 클러스터링 규격(필터는 geometryFunction에서, 재그리기는 refreshFacilityLayer)과 패널 컨벤션 추가 |

## 검증 (로컬, 헤드리스 Chrome + CDP)

| 확인 | 결과 |
|---|---|
| `node --check map-facility.js` | 통과 |
| 묶음 켬(줌 11.5) | 248건 → **39개 묶음**, 가장 큰 묶음 33건 |
| 묶음 클릭 | 줌 **11.54 → 15.8** (범위로 확대됨) |
| 토글 끔 | 소스가 원본으로 바뀌고 **개별 핀 248개**, `localStorage=off` 저장 |
| 다시 켬 | 묶음 108개(줌이 높아 잘게 묶임), 총원 248 유지 |
| 필터 × 묶음 일치 | 전체 248/248, 보수 필요 1/1, 처리 대기 0/0, 보수 불필요 247/247 — **묶음 소속 피처 수 = 화면 목록 건수** 전부 일치 |
| 화면 캡처 | 개수 배지(2·3·5·10·14·21·23…)와 단독 핀이 함께 정상 표시 |

## 되돌린 작업 (trash 이동 기록)

이 작업 직전에 착수했던 **현장 재점검 요청 기능**을 사용자 지시로 중단하고, 그때까지 새로 만든 5개 파일을 삭제하지 않고 `trash/2026-09-23/` 아래에 원래 경로 구조로 옮겼습니다(프로젝트 규칙). 기존 파일은 수정하지 않았고 DB 작업도 하지 않았습니다.

```
trash/2026-09-23/db/map_facility_reinspection.sql
trash/2026-09-23/src/main/java/com/example/mapservice/mapper/QfieldReinspectionMapper.java
trash/2026-09-23/src/main/java/com/example/mapservice/service/QfieldReinspectionService.java
trash/2026-09-23/src/main/java/com/example/mapservice/service/impl/QfieldReinspectionServiceImpl.java
trash/2026-09-23/src/main/resources/mapper/qfield-reinspection.xml
```

## 남은 작업

- 실제 브라우저에서 눈으로 확인(로컬 스택은 기동되어 있음).
- 배지 색 규칙은 "묶음 안에 처리할 보수 건이 하나라도 있으면 주황"입니다 — 운영에서 써 보고 기준을 바꿀지 판단.
- 커밋·push 미진행.
