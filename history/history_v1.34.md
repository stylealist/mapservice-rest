# history v1.34 — 상세 팝업에 "보수 불필요"도 표시

- **날짜**: 2026-09-17
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.33.md](history_v1.33.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 프론트엔드 | http://localhost:4000 | 시설물 선택 → 상세 팝업 헤더·"보수 필요 여부" 행 (반영은 Ctrl+F5) |

## 실행한 프롬프트

```
상세 팝업에 보수 필요는 나오는데 보수 불필요는 안나오고 있어 나오도록 해줘
```

## 원인

- **헤더 배지**: `renderFacilityPopupHeader()`가 `repair_required_yn === 'Y'`일 때만 `보수 필요` 배지를 넣고, 그 밖에는 아무것도 넣지 않았습니다.
- **본문 "보수 필요 여부" 행**: 실데이터 대부분이 빈 값이라 `-`로 나왔고, 값이 있어도 앱 ValueMap 원문(`Y`=정비요청, `N`=양호)이라 v1.33 필터의 용어(보수 필요/보수 불필요)와 달랐습니다.

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-facility.js` | 헤더에 `Y`가 아니면 `보수 불필요` 배지 추가. `FACILITY_VALUE_MAPS.repair`를 Y=보수 필요, N=보수 불필요로 변경. `DETAIL_FIELD_CONFIG`에 `emptyValue` 옵션 추가 — `repair_required_yn`은 빈 값을 `N`으로 해석 |
| `css/components/layer-panel.css` | `.facility-badge.badge-no-repair`(초록) 추가 |
| `docs/map-architecture.md` | 보수 필요 여부 표시 규칙(항상 표시, 필터와 같은 기준, 앱 원문 용어) 추가 |

- 판정 기준은 v1.33 목록 필터와 같습니다: **`Y`면 보수 필요, 그 밖(`N`·빈 값)은 보수 불필요.**
- 배지 색: 보수 필요 = 주황(기존, 핀 경고색), 보수 불필요 = 초록(경고와 구분).

## 검증 결과 (실제 Chrome 1400×950)

| 시설물 | `repair_required_yn` | 헤더 배지 | 본문 "보수 필요 여부" |
|---|---|---|---|
| Test | `Y` | 시설물 · **보수 필요**(주황) | 보수 필요 |
| 본사 | 빈 값 | 시설물 · **보수 불필요**(초록) | 보수 불필요 (이전: `-`) |

콘솔·페이지 오류 0건.

## 참고

- 목록 항목의 오른쪽 배지는 기존대로 보수 필요일 때만 `보수필요`, 아니면 시설물 상태를 표시합니다(이번 요청 범위 밖).
- v1.35 작업 후 요청에 따라 v1.33~v1.35를 함께 커밋·푸시했습니다([history_v1.35.md](history_v1.35.md) 참고).
