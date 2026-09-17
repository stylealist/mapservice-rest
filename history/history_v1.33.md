# history v1.33 — 시설물 검색에 보수 필요 여부 필터(전체 / 보수 필요 / 보수 불필요) 추가

- **날짜**: 2026-09-17
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.32.md](history_v1.32.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 프론트엔드 | http://localhost:4000 | 좌측 시설물 탭 → 검색창 아래 필터 (반영은 Ctrl+F5) |
| 시설물 API | http://localhost:8100/map/qfield/facilities | 변경 없음 (`repair_required_yn` 이미 포함) |

## 실행한 프롬프트

```
검색에서 기본값은 전체이고 보수필요, 보수불필요 선택해서 볼 수 있도록 해줘
```

## 데이터 확인

게이트웨이로 목록 API를 조회해 `repair_required_yn` 값을 집계했습니다.

| 범위 | `Y` | 빈 값 |
|---|---|---|
| 전국 | 2 | 2,473 |
| 서울 | 1 | 247 |

`N`은 없고 대부분 빈 값이라, **보수 불필요 = `Y`가 아닌 전부**로 판정했습니다(`N`만 보면 0건).

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `index.html` | 검색창 아래 세그먼트 버튼 `#facilityRepairFilter`(전체·보수 필요·보수 불필요, 옆에 건수) 추가 |
| `js/modules/map/map-facility.js` | `facilityRepairFilter` 상태, `matchesFacilityRepairFilter()`, `bindFacilityRepairFilter()`, `updateFacilityRepairCounts()` 추가. `renderFacilityList()`와 `facilityStyleFunction()`에 필터 적용 |
| `css/components/layer-panel.css` | 세그먼트 버튼 스타일(보수 필요 선택 시 핀 경고색), 검색어 지우기 버튼 `.hidden` 규칙 추가 |
| `docs/ui-conventions.md` | 필터 동작·판정 기준·지도 적용·건수 기준·레이아웃 주의 추가 |

- **기본값은 전체**. 서버를 다시 부르지 않고 받아 둔 목록에서 거릅니다.
- **목록과 지도 핀에 함께 적용**합니다(검색어는 기존대로 목록에만). 필터에서 빠진 핀은 그리지 않아 클릭 대상에서도 빠지며, 열려 있던 팝업의 시설물이 빠지면 팝업을 닫습니다.
- 버튼 옆 건수는 검색어까지 반영한 기준이라, 어느 쪽에 결과가 있는지 바로 보입니다.
- 행정구역을 바꿔 다시 조회해도 선택한 필터는 유지됩니다.

### 함께 고친 기존 버그

- 검색어가 비어 있어도 **검색어 지우기 버튼(×)이 항상 보이던** 문제 — JS는 `hidden` 클래스를 토글하는데 이 버튼용 CSS 규칙이 없었습니다(v1.10부터). `.facility-keyword-clear.hidden { display: none; }` 추가.

## 검증 결과 (실제 Chrome 1400×950)

| 단계 | 선택 | 목록 | 지도 핀 | 버튼 건수(전체/필요/불필요) | 비고 |
|---|---|---|---|---|---|
| 첫 화면(서울) | 전체 | 248 | 248 | 248 / 1 / 247 | 기본값 전체 |
| 보수 필요 | 필요 | 1 | 1 | 248 / 1 / 247 | |
| 그 항목 선택 | 필요 | 1 | 1 | | 팝업 열림 |
| 보수 불필요로 전환 | 불필요 | 247 | 247 | | **팝업 자동 닫힘** |
| + 검색어 "주차" | 불필요 | 54 | 247 | 54 / 0 / 54 | 검색어는 목록만 |
| 보수 필요 + "주차" | 필요 | 0 | 1 | 54 / 0 / 54 | "검색 결과가 없습니다" 표시 |
| 전체로 복귀 | 전체 | 248 | 248 | 248 / 1 / 247 | |
| 시·도 전체로 재조회 | 필요(유지) | 2 | 2 | 2,475 / 2 / 2,473 | 필터 유지 |

- 콘솔·페이지 오류 0건
- 전국 조회 시 "보수 불필요 2,473"이 처음엔 버튼 너비(92px)를 넘쳐, 칸을 글자 길이에 맞춰 나누도록 수정 → 넘침 없음(76 / 83 / 117px)
- 검색어 지우기 버튼: 검색어 없음 → 숨김, 입력 → 표시

## 참고

- v1.35 작업 후 요청에 따라 v1.33~v1.35를 함께 커밋·푸시했습니다([history_v1.35.md](history_v1.35.md) 참고).
