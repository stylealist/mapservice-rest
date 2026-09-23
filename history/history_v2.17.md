# history v2.17 — 시설물 핀·묶음이 WFS 아이콘에 가리던 문제 해결(declutter)

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.16.md](history_v2.16.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 편의점 레이어를 켜고 확인 |
| 운영 지도 | https://sj-lab.co.kr/map/ | |

## 실행한 프롬프트

```
(스크린샷 첨부) 현재도 시설물 핀과 클러스터들이 다른 레이어 아이콘보다 아래있어서 가려지고있어
```

## 원인 — zIndex 문제가 아니었다

v2.16에서 시설물 레이어를 `zIndex 1500`(다른 레이어 최대 1000)으로 올렸는데도 편의점(WFS) 아이콘이 시설물 위에 그려졌습니다. 벤더링된 OpenLayers 렌더러를 확인한 결과:

```js
// 모든 레이어를 zIndex 순으로 그린 뒤,
for (...) { layer.render(frameState); if ("getDeclutter" in layer) declutterLayers.push(layer); }
// declutter 레이어의 심볼만 마지막에 다시 그린다
for (let i = declutterLayers.length - 1; i >= 0; --i) declutterLayers[i].renderDeclutter(frameState);
```

**declutter 레이어의 심볼은 프레임 마지막에 따로 그려지므로 zIndex 와 무관하게 위로 올라옵니다.** WFS 레이어는 `declutter: true`, 시설물 레이어는 `declutter: false`였기 때문에 아무리 zIndex 를 올려도 가려졌습니다.

## 조치

| 항목 | 파일 | 내용 |
|---|---|---|
| declutter 참여 | `js/modules/map/map-facility.js` | 시설물 레이어를 `declutter: true` 로 바꾸고, 모든 시설물 스타일(핀 아이콘·선택 링·묶음 배지)에 **`declutterMode: "obstacle"`**(`FACILITY_DECLUTTER_MODE`) 지정. `obstacle` 은 **항상 그리되 다른 declutter 심볼이 피해 가게** 하는 모드라, 시설물은 하나도 숨지 않고 겹치는 WFS 아이콘 쪽이 밀려남 |
| 묶음 배지 | 〃 | 개수 숫자를 `ol.style.Text` 로 얹으면 declutter 대상이 되어 **같은 자리의 원(obstacle)과 충돌해 사라짐** → 원+숫자를 한 장으로 그리는 SVG 아이콘(`buildFacilityClusterIconUrl()`)으로 교체 |
| 문서 | `docs/map-architecture.md` | "declutter: false" 규격을 "declutter: true + obstacle"로 교체하고 이유(렌더 순서)와 금지 사항 명시. 배지를 SVG 한 장으로 그리는 이유도 기록 |

zIndex 1500 + 자동 보정(v2.16)은 그대로 둡니다 — 두 장치가 함께 있어야 일반 레이어·declutter 레이어 양쪽 모두에 대해 최상단이 보장됩니다.

## 검증 (로컬, 헤드리스 Chrome + CDP)

| 확인 | 결과 |
|---|---|
| 편의점(WFS) 레이어 켠 상태 | 시설물 묶음 배지가 **편의점 아이콘 위**에 표시, 겹치던 아이콘은 밀려남(캡처 확인) |
| 묶음 개수 숫자 | 조치 전 사라짐 → SVG 교체 후 **정상 표시**(2·3·5·6·7·8·9·15·23 …) |
| 클러스터 끔 + 줌 15 | 개별 핀도 편의점 아이콘 위에 표시 |
| 건수 일치 | 시설물 피처 248 = 화면 목록 248 (obstacle 이라 숨김 없음) |
| 레이어 상태 | 시설물 `zIndex 1500`, `declutter true`, 편의점 `zIndex 1000`, `declutter true` |
| `node --check` | 통과 |

## 남은 작업

- v2.15~v2.17을 함께 커밋·push.
- 다른 WFS 레이어(버스정류장·CCTV·병원 등)를 동시에 켠 상태에서도 육안 확인.
