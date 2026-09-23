# history v2.10 — 내업 완료 건을 보수 필요 목록에서 어떻게 다룰지 (현행 유지 + 보완)

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(문서·기록)
- **이전 버전**: [history_v2.9.md](history_v2.9.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 시설물 탭 → 보수 필요 → 내업상태 |
| 로컬 로그인 페이지 | http://localhost:8100/auth/login.html | 로그인 게이트 통과용 |
| 로컬 API(게이트웨이) | http://localhost:8100/map/qfield/facilities | 이번 변경에 API 수정 없음 |
| 운영 지도 | https://sj-lab.co.kr/map/ | |

## 실행한 프롬프트

```
서비스 로컬에서 확인가능하도록 서버 기동해줘
현재 보수 필요에서 내업 완료를 했을때도 보수필요탭에 나오는데 그렇게 나오는게 맞을까? 아니면 내업 완료시에는 보수 불필요로 나오는게 맞을까? 아니면 별도의 탭을 두는게 맞을까?
현행 유지 + 작은 보완 진행해줘
```

## 판단 (코드·데이터 확인 후)

| 선택지 | 판단 | 근거 |
|---|---|---|
| 내업 완료 시 **보수 불필요로 바꾸기** | ❌ | `repair_required_yn`은 **외업(현장조사 앱 → QFieldCloud → `qfield.facility_total_view`)** 값이라 이 서비스의 쓰기 대상이 아니다. 억지로 바꿔도 `sj-qfieldsync`의 다음 동기화(30초 주기)에 덮어써진다. 의미상으로도 내업 완료는 *사무실 처리* 완료이지 *현장 보수가 끝나 재점검으로 확인됨*이 아니다(그건 다음 현장조사에서 `N`으로 갱신되는 것이 정상 순환) |
| **별도 탭** 신설 | ❌ | 세그먼트(전체/보수 필요/보수 불필요)는 *현장 판정* 축이고 내업 상태는 *처리 진행* 축이다. 한 줄에 섞으면 배타 선택이 깨지고 건수도 어긋난다(내업 완료 건은 보수 필요이기도 하므로) |
| **현행 유지 + 보완** | ✅ 채택 | 두 축 구조는 그대로 두고, "아직 처리할 건"만 보는 수단과 목록 순서만 보완 |

## 작업 내용

| 항목 | 파일 | 조치 |
|---|---|---|
| 내업상태 필터에 `처리 대기(완료 제외)` 추가 | `sj-lab-mapservice/index.html`, `js/modules/map/map-facility.js` | 상태 코드가 아닌 묶음 값 `OFFICE_WORK_OPEN_FILTER = "OPEN"` 신설. 내업 대상(보수 필요)이면서 `DONE`이 아닌 전부(미완료·접수·처리중·보류)를 남긴다. 기본값은 기존대로 `전체`(완료 건도 보이는 현행 유지) |
| 필터 건수 | `map-facility.js` `renderFacilityList()` | select 항목 뒤 건수 계산에 `OPEN` 집계 추가(예: `처리 대기(완료 제외) (85)`). 조회 0건일 때 초기화하던 옛 키(`done`/`notDone`)도 정리 |
| 목록 정렬 | `map-facility.js` `renderFacilityList()` | 필터를 통과한 항목 중 **내업 완료를 맨 뒤로** 보내는 안정 정렬(나머지는 조회 순서 유지). 남은 일이 위로 모인다. 지도 핀에는 영향 없음 |
| 판정 헬퍼 | `map-facility.js` | `isOfficeWorkDone(needsRepair, status)` 추가 — 완료 판정을 `resolveOfficeWorkStatus()` 한 곳으로 통일 |
| 문서 | `sj-lab-mapservice/docs/ui-conventions.md` | 내업 상태 필터 항목에 `처리 대기(완료 제외)`의 의미와 "작성 폼 선택지에 넣지 말 것", 목록 정렬 규칙 추가 |

**백엔드·API 변경 없음** — `office_work_status`를 이미 내려주고 있어 클라이언트 필터만으로 처리했습니다. `map.facility_office_work`, `qfield.facility_total_view` 모두 건드리지 않았습니다.

## 검증

| 확인 | 결과 |
|---|---|
| `node --check js/modules/map/map-facility.js` | 통과 |
| 로컬 스택 기동(`scripts\local-stack.ps1 start`) | eureka·mapservice-rest·authserver·apigateway·frontend 전부 실행, Eureka 4개 UP |
| 게이트웨이 경유 API | `GET /map/admin-area/sido` 200, 로그인 페이지 200, `POST /auth/login/demo` 200 |
| 필터 로직 | `전체`는 기존과 동일(완료 포함), `처리 대기`는 완료만 빠짐, `완료`는 완료만 — 보수 불필요 시설물은 어느 경우에도 내업 필터 대상이 아님(`resolveOfficeWorkStatus()`가 `""`) |

## 남은 작업

- 브라우저에서 시설물 탭 → `보수 필요` → `처리 대기(완료 제외)` 동작 육안 확인(로컬 스택은 기동해 둠).
- 기본값을 `처리 대기`로 둘지는 운영 사용 후 결정(지금은 현행 유지를 위해 `전체`).
- 실제 보수가 끝난 시설물의 `repair_required_yn`은 다음 현장조사 때 외업에서 `N`으로 갱신되어야 목록에서 빠진다 — 현장 운영 절차와 맞는지 확인 필요.
- (이전 버전에서 이어짐) 배포 시 `sj-lab-mapservice`가 지워지는 원인 조사, single-logout 미구현, 백엔드 API 토큰 미강제.
