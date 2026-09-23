# history v2.18 — 확대해도 풀리지 않는 묶음을 부채꼴로 펼치기(spiderfy)

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.17.md](history_v2.17.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 같은 자리 묶음 배지를 눌러 확인 |
| 운영 지도 | https://sj-lab.co.kr/map/ | |

## 실행한 프롬프트

```
(스크린샷 첨부) 클러스터에서 끝까지 확대를해도 핀으로 나오지않는 시설물들도있어
```

## 문제

한 건물에 여러 시설물이 있는 경우 좌표가 **완전히 동일**합니다(실측: 13건 묶음의 extent 폭·높이 = 0). 묶음 클릭은 "그 범위로 확대"였으므로 **최대 배율(19)까지 확대해도 묶음이 풀리지 않아 그 시설물들을 지도에서 고를 수 없었습니다**.

## 조치

| 항목 | 파일 | 내용 |
|---|---|---|
| 펼치기 | `js/modules/map/map-facility.js` | 묶음 구성원의 extent 가 화면 1픽셀(`resolution`)보다 작거나 이미 최대 배율이면 확대 대신 **중심에서 원형으로 핀을 펼치고 연결선**을 그림(`spiderfyFacilityCluster()`). 반지름은 개수에 따라 46px + (n-4)×5px |
| 전용 레이어 | 〃 | `facilitySpiderLayer`(zIndex = 시설물 + 1, 시설물과 같은 obstacle declutter). 펼친 핀은 **원본 피처 참조(`__facilityFeature`)만** 들고 있어 스타일·클릭이 기존 규칙 그대로. 연결선은 `__spiderLine` |
| 접기 | 〃 | 같은 묶음 재클릭(`facilitySpiderKey` 비교), 배율 변경(`change:resolution`), 필터 변경, 목록 재조회, 묶음 토글 끔 → 모두 접음 |
| 클릭 순서 | 〃 | **펼친 핀 → 묶음/핀** 순으로 히트 테스트(펼친 핀이 배지 위에 떠 있으므로). 펼친 핀 클릭은 기존과 같이 `selectFacility()` |
| zIndex 보정 | 〃 | `keepFacilityLayerOnTop()`의 최대값 계산에서 펼침 레이어 **제외** — 넣으면 두 레이어가 서로를 밀어 올려 zIndex 가 무한히 커짐 |
| 문서 | `docs/map-architecture.md`, `docs/ui-conventions.md` | 펼치기 규칙·접히는 조건·클릭 순서·zIndex 주의 추가 |

## 검증 (로컬, 헤드리스 Chrome + CDP)

| 확인 | 결과 |
|---|---|
| 같은 자리 묶음 탐지 | 서울 데이터에서 extent 0 인 묶음 3건(3·3·2건) 확인 |
| 묶음 클릭 | 핀 3 + 연결선 3 생성 (캡처로 부채꼴 배치 확인) |
| 펼친 핀 클릭 | 해당 시설물이 목록에서 선택됨(`.facility-item.selected` 1건) |
| 같은 배지 재클릭 | 6 → 0 → 6 (펼침/접힘 토글 정상) |
| 배율 변경 | 자동으로 접힘(0) |
| `node --check` | 통과 |

**참고**: 좌표가 같은 묶음은 배율과 무관하게 즉시 펼쳐집니다(확대해도 갈라지지 않으므로). 좌표가 다른 묶음은 종전대로 범위 확대입니다.

## 남은 작업

- v2.15~v2.18을 함께 커밋·push.
- 펼침 반지름·연결선 색은 실제 사용 후 조정 가능.
