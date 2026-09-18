# history v2.0 — 시설물 내업(사무실 처리) 기능 신설: DB · 백엔드 API · 프론트엔드

외업(QField 앱)에서 보수 요청한 시설물을 내업에서 웹으로 확인하고 처리 정보를 작성하는 흐름을 처음부터 만들었습니다.
DB 테이블, 이 저장소 최초의 **쓰기 API**, 프론트 작성 화면이 함께 추가되어 major 를 올립니다.

- **날짜**: 2026-09-18
- **영향 저장소**: `mapservice-rest`(DB 스크립트·백엔드), `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.35.md](history_v1.35.md)
- **진행 방식**: Orca 오케스트레이션 — Claude 워커(DB+백엔드) · Antigravity 워커(프론트엔드) 병렬, 최종 검증은 Claude(코디네이터)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 프론트엔드 | http://localhost:4000 | 보수 필요 시설물 선택 → 팝업 "내업 처리" 섹션 |
| 내업 API | http://localhost:8100/map/qfield/facilities/{totalId}/office-works | 게이트웨이 경유 |

## 실행한 프롬프트

```
/orchestration 내가 만드는 프로젝트는 외업에서 앱으로 시설물 관리 조사를 해서 보수 요청을 한 부분에 대해서 웹사이트에서 확인후에 추가 정보를 작성하여 처리하는 flow로 진행되려고하는데 지금 내업에서 추가 정보 작성하는 부분에 대해서 DB도 없고 서버, 프론트에도 작성이 되있지않는 상황이야 시설물관리 내업에서 작성할 테이블을 외래키를 qfield.facility_total_view 테이블의 total_id로 해서 db 작성, 백엔드, 프론트엔드를 작성해줘 그리고 보수필요 항목들중에서 내업을 완료했을시에 완료했다는 느낌의 텍스트도 나왔으면 좋겠어 claude와 antigravity를 사용해서 오류 없이 진행해줘
```
```
두 워커 다 끝나면 통합 검증해줘
```
```
보수필요일때만 전체, 미완료, 접수, 처리중, 완료, 보류 설정 부분이 나왔으면 좋겠어 그리고 뭔가 너무 복잡한 느낌이야
```
```
<label for="facilityOfficeSelect">내업</label>을 내업상태로 바꿔주고 commit push해줘 그리고 다른 진행중인 워커들 종료해줘
```

사용자가 고른 설계: 항목은 기본 처리 정보 + 업체·사진, 시설물당 **여러 건 이력**, 완료 표시는 목록·팝업·필터·핀 색 4곳, DB 는 스크립트 작성 후 **실행까지 진행**.

## 중요한 발견 — 앱 테이블은 `qfield` 스키마에 두면 안 된다

처음에는 요청대로 `qfield.facility_office_work` 로 만들었지만, **`sj-qfieldsync` 의 `cleanup_deleted_projects` 가 `qfield` 스키마에서 QField 프로젝트 이름 패턴이 아닌 테이블을 "삭제된 프로젝트 테이블"로 보고 아카이브 후 DROP** 합니다. 실제로 내업 테이블과 기존 `qfield.facility_icon` 이 삭제됐습니다(아이콘 API 가 0건을 반환하던 원인).

그래서 두 테이블 모두 **`map` 스키마로 옮겼습니다**.

| 이전 | 이후 |
|---|---|
| `qfield.facility_office_work` | **`map.facility_office_work`** |
| `qfield.facility_icon` | **`map.facility_icon`** (7건 복구) |

`total_id` 가 가리키는 `qfield.facility_total_view` 는 뷰라서 물리적 외래키 제약을 걸 수 없습니다. 논리적 FK + 인덱스 + 백엔드 존재 검증으로 구현했습니다.

## 변경된 결과물

### DB (`mapservice-rest/db`)

| 파일 | 내용 |
|---|---|
| `db/map_facility_office_work.sql` | **신규** — `map.facility_office_work` 생성. work_id(PK) · total_id(논리 FK) · work_status(CHECK: RECEIVED/IN_PROGRESS/DONE/HOLD) · 처리내용 · 부서 · 담당자 · 연락처 · 예정일 · 완료일 · 비용 · 업체 · 계약번호 · 처리 전후 사진 경로 · 비고 · use_yn · reg_date · update_at, 부분 인덱스 `(total_id, work_id DESC) WHERE use_yn='y'` |
| `db/map_facility_icon.sql` | **신규** — 아이콘 테이블을 `map` 스키마로 옮긴 스크립트 |
| `trash/2026-09-18/db/qfield_facility_office_work.sql` | 이동(삭제 대신) — 구 `qfield` 스키마용 |
| `trash/2026-09-18/db/qfield_facility_icon.sql` | 이동(삭제 대신) — 구 `qfield` 스키마용 |

개발 DB(`sjlab`, 백엔드 datasource 계정)에 사용자 승인으로 실행 완료. 비밀번호는 출력하지 않았습니다.

### 백엔드 (`mapservice-rest`)

| 파일 | 내용 |
|---|---|
| `QfieldOfficeWorkController/Service/ServiceImpl/Mapper` + `mapper/qfield-office-work.xml` | **신규** — 내업 기록 조회·등록·수정·삭제 |
| `QfieldFacilityServiceImpl`·`QfieldFacilityMapper`·`qfield-facility.xml`·`QfieldFacilityController` | 시설물 목록에 `office_work_status`·`office_work_complete_date` 추가(LEFT JOIN LATERAL), **테이블 없을 때 폴백**(`to_regclass` 확인 후 두 키를 null 로) |
| `CLAUDE.md`, `docs/system-architecture.md` | 쓰기 API 계층·스키마 주의·API 계약 표 갱신 |

API (게이트웨이 기준):

| 메서드 | 경로 | 응답 |
|---|---|---|
| GET | `/map/qfield/facilities/{totalId}/office-works` | 200 `{totalId, items[]}` (최신순), 시설물 없으면 404 |
| POST | `/map/qfield/facilities/{totalId}/office-works` | 201 + 생성 항목, 검증 실패 400, 시설물 없으면 404 |
| PUT | `/map/qfield/office-works/{workId}` | 200 + 수정 항목, 없으면 404 |
| DELETE | `/map/qfield/office-works/{workId}` | 204(소프트 삭제), 없으면 404 |

쓰기 응답은 `Cache-Control: no-store`. `work_status` 화이트리스트·날짜 형식·비용 자릿수·`DONE`이면 완료일 필수를 서버에서 검증합니다.

### 프론트엔드 (`sj-lab-mapservice`)

| 파일 | 내용 |
|---|---|
| `js/modules/map/map-facility.js` | 팝업 "내업 처리" 섹션(최신 1건 + 이력 아코디언, 작성·수정·삭제 폼), 내업 상태 필터, 배지·핀 색 |
| `index.html`, `css/components/layer-panel.css` | 내업 상태 필터 줄, 섹션·폼·배지 스타일 |
| `js/modules/map/map.js` | 전역 등록 |
| `docs/ui-conventions.md`, `docs/map-architecture.md` | 내업 UI·필터·배지·핀 색 규칙 |

완료 표시 4곳: 목록 배지 · 팝업 헤더 배지(`내업 완료 · 완료일 · 담당자`) · 검색 필터 · 지도 핀 색(보수 필요 주황 → 내업 완료 초록 `#059669`).

**필터 단순화(사용자 요청)**: 내업 상태 필터는 **"보수 필요"를 고른 경우에만** 나오며, 6칸 세그먼트 두 줄 대신 한 줄 `내업상태` select(전체/미완료/접수/처리중/완료/보류, 항목마다 건수)로 뒀습니다. 보수 필요를 벗어나면 선택이 자동으로 "전체"로 돌아갑니다. 기본 화면의 검색 영역 높이가 318px → 245px로 줄었습니다.

## 검증 결과 (게이트웨이 + 실제 Chrome 1500×1000)

| 확인 | 결과 |
|---|---|
| 등록·조회·수정·삭제 | 201 / 200 / 200 / 204, 한글 저장·조회 정상 |
| 오류 처리 | 없는 시설물 404, 잘못된 상태값 400, 잘못된 날짜 400, 없는 기록 PUT·DELETE 404 |
| 시설물 목록 | 248건 유지, `office_work_status` 반영 |
| 아이콘 API | 7건 (스키마 이관으로 복구, 이전 0건) |
| 목록·팝업 배지, 핀 색 | 저장 즉시 반영, 보류로 바꾸면 주황 복귀 |
| 필터 | 기본 숨김 → 보수 필요 선택 시 표시, 미완료/완료로 목록 필터링, 보수 불필요 전환 시 전체로 초기화 |
| 폼 검증 | 완료인데 완료일 없으면 "완료 상태인 경우 완료일을 입력해 주세요." |
| 이력 | 최신 기록 삭제 시 이전 기록 상태로 복귀 |
| 콘솔·페이지 오류 | 0건 |

검증용 기록은 모두 소프트 삭제해 DB 는 활성 0건입니다.

## 리뷰에서 나온 사항

- **고침**: 시설물 목록이 새 테이블에 폴백 없이 의존 → `to_regclass` 확인 후 폴백 쿼리. `DONE` 완료일 검증을 백엔드에도 추가.
- **남김(사용자 결정)**: 이번이 최초의 쓰기 API 인데 인증·인가가 없습니다. 게이트웨이 앞단에서 한 번에 다루기로 하고 이번 범위에서는 그대로 뒀습니다.

## 진행 중 있었던 일

- **DB 잠금**: 사용자 DBeaver 세션이 15분 넘게 트랜잭션을 열어 둔 채 테이블 잠금을 쥐어 `sj-qfieldsync` 작업과 시설물 목록 조회가 대기했습니다. 확인 시점에는 이미 풀려 있어 아무 세션도 종료하지 않았습니다.
- **워커 종료**: 사용자 요청으로 진행 중이던 후속 워커(dispatch `ctx_96fda7e32046`)를 `worker-abandon` 후 두 워커 창을 닫았습니다. 스키마 이관은 DB·코드까지 반영된 상태였고, 남은 빌드·재기동·검증은 코디네이터가 마쳤습니다.

## 배포 시 주의

1. 운영 DB 에 `db/map_facility_office_work.sql`, `db/map_facility_icon.sql` 을 **먼저 실행**할 것. 실행 전에도 시설물 목록은 폴백으로 동작하지만(`office_work_status: null`) 내업 기록은 저장·조회되지 않습니다.
2. 앱이 쓰는 테이블을 `qfield` 스키마에 만들지 말 것 — `sj-qfieldsync` 가 삭제합니다.
