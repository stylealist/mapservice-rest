# history v2.25 — 로고 클릭 = 지도 사이트 새로 불러오기로 확정

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.24.md](history_v2.24.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 로고 클릭 시 이 주소를 다시 불러옴 |
| 운영 지도 | https://sj-lab.co.kr/map/ | 로고 클릭 시 이 주소를 다시 불러옴 |

## 실행한 프롬프트

```
logoHome를 누르면 로컬이든 실서버든 /map 웹사이트를 다시 불러와야해
```

## 작업 내용

로고 동작이 세 번 바뀐 끝에 **"지도 사이트를 통째로 다시 불러오기"**로 확정됐습니다.

| 버전 | 동작 | 문제 |
|---|---|---|
| ~v2.22 | `origin + "/map"` 으로 이동 | 운영에만 있는 경로 → 로컬에서 **404** |
| v2.23 | SPA 페이지 전환 | 이미 지도 화면이면 **아무 변화 없음** |
| v2.24 | 전환 + 첫 화면 복귀(`resetMapView`) | 요구사항과 다름(사이트를 다시 불러오길 원함) |
| **v2.25** | **현재 디렉터리를 다시 불러오기** | — |

| 항목 | 파일 | 내용 |
|---|---|---|
| 로고 동작 | `js/modules/ui.js` | `goToMap()`이 `window.location.origin + pathname.replace(/[^/]*$/, "")` 로 이동 — **지금 문서가 있는 디렉터리 기준**이라 운영 `https://sj-lab.co.kr/map/`, 로컬 `http://localhost:4000/` 양쪽에서 맞는다. 쿼리·해시(로그인 토큰 등)는 떨어짐 |
| v2.24 되돌림 | `js/modules/map/map-core.js`, `map.js` | 더 이상 쓰지 않는 `resetMapView()`와 `window` 등록 제거. 기본 중심·배율 상수(`DEFAULT_MAP_CENTER`/`DEFAULT_MAP_ZOOM`)는 지도 생성에 쓰이므로 유지 |
| 문서 | `docs/ui-conventions.md` | 로고 규칙을 새로고침 방식으로 갱신하고, **절대 경로 하드코딩 금지**(404)와 **SPA 전환만으로는 무반응**이라는 두 실패 사례를 함께 기록 |

## 검증 (로컬, 헤드리스 Chrome + CDP)

| 단계 | 결과 |
|---|---|
| 소개 탭 + 부산으로 이동·확대 | `about-page`, 경도 129.07, 배율 15 |
| **로고 클릭** | 사이트 재로딩 → `map-page`, 경도 **126.89**, 배율 **11.3**(첫 화면), URL `http://localhost:4000/` |
| `node --check` | `ui.js`·`map.js`·`map-core.js` 통과 |

**검증 중 확인한 함정(제품 버그 아님)**: 테스트가 로그인 토큰을 해시로 넣을 때 `auth_expires` 를 빠뜨리면 `auth-gate.js`가 `expiresAt = 지금`으로 저장해 **새로고침 즉시 로그인 페이지로 튕깁니다**. 게이트 동작은 정상이며, 토큰을 수동으로 주입해 테스트할 때는 `auth_token`·`auth_expires`·`auth_username`을 함께 넣어야 합니다.

## 남은 작업

- 운영 배포 후 로고 클릭 육안 확인(`/map/`을 다시 불러오는지).
