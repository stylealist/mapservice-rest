# history v1.6 — 시설물 아이콘 설정을 DB(qfield 스키마)로 이관

- **날짜**: 2026-09-16
- **영향 저장소**: `mapservice-rest`(백엔드·SQL), `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.5.md](history_v1.5.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 프론트엔드 | http://localhost:4000 | 시설물 탭 |
| 아이콘 설정 API | http://localhost:8100/map/qfield/facility-icons | 게이트웨이 경유 (신규) |

## 실행한 프롬프트

```
아이콘 관련을 모두 하드코딩하는건 좋은방법이 아닌거같아 db의 qfield 스키마에 db화 해서 관리해줄수있어?
```

> **후속**: 이 스크립트는 2026-09-16 사용자 지시로 실행 완료되었습니다 — [history_v1.7.md](history_v1.7.md) 참고.

## 남은 작업 (담당자 실행 필요)

프로젝트 규칙상 에이전트는 DDL을 실행하지 않습니다. **아래 스크립트를 DB 권한이 있는 담당자가 직접 실행**해야 DB 기반 아이콘이 적용됩니다.

```
psql -h <호스트> -p <포트> -U <계정> -d sjlab -f db/qfield_facility_icon.sql
```

실행 전에도 지도는 정상 동작합니다(내장 기본 아이콘 사용).

## 변경된 결과물

| 파일 | 구분 | 내용 |
|---|---|---|
| `db/qfield_facility_icon.sql` | 신규 | `qfield.facility_icon` 생성 + 초기 데이터 7건(UPSERT). 담당자가 실행 |
| `src/main/resources/mapper/qfield-facility.xml` | 수정 | `getFacilityIcons` select 추가 (`json_agg`으로 설정을 JSON 배열로 반환) |
| `mapper/QfieldFacilityMapper.java` · `service/QfieldFacilityService.java` | 수정 | `getFacilityIcons()` 선언 추가 |
| `service/impl/QfieldFacilityServiceImpl.java` | 수정 | 조회 실패 시 경고 로그 + `null` 반환(지도가 멈추지 않도록) |
| `controller/QfieldFacilityController.java` | 수정 | `GET /qfield/facility-icons` 추가, `Cache-Control` 3600초, 값이 없으면 `[]` |
| `js/modules/map/map-facility.js`(프론트) | 수정 | `loadFacilityIconConfig()`로 DB 설정 로드, 하드코딩 배열은 대체값으로 강등 |
| `CLAUDE.md`, `docs/system-architecture.md`, 프론트 `docs/map-architecture.md` | 수정 | 설정 테이블·API·아이콘 관리 기준 문서화 |
| `docs/dev-environment.md` | 수정 | 백엔드만 재기동했을 때 Eureka 유령 인스턴스 대처법 추가 |

### 테이블 구조 (`qfield.facility_icon`)

| 컬럼 | 용도 |
|---|---|
| `icon_type` | 아이콘 식별자(`parking`, `charger`, `hall`, `dining`, `sports`, `exhibition`, `default`) |
| `icon_label` | 팝업 배지에 표시할 종류 이름 |
| `keywords` | `text[]` — `fclt_nm`에 이 단어가 있으면 해당 아이콘 사용 |
| `glyph_svg` | 핀 안에 그릴 SVG 조각. 문자열 `COLOR`가 핀 색으로 치환됨 |
| `pin_color` / `warn_color` | 기본 핀 색 / 보수 필요(`repair_required_yn='Y'`) 핀 색 |
| `sort_order` | 키워드 판별 우선순위 |
| `is_default` | 종류 미판별 시 사용할 기본 아이콘(부분 유니크 인덱스로 한 건만 허용) |
| `use_yn` | `'n'`이면 사용하지 않음 |

기존 `qfield.qfield_info`와 같은 스키마·명명 방식(스네이크 케이스, `use_yn`, `reg_date`/`update_at`)을 따랐습니다.

### 동작 방식

1. 프론트 초기화 때 `loadFacilityIconConfig()`가 `GET /map/qfield/facility-icons` 호출
2. 응답이 있으면 `facilityIconTypes`·`facilityDefaultIcon`을 교체하고 `facilityStyleCache`를 비운 뒤 레이어를 다시 그림
3. 실패하거나 응답이 비면 `FALLBACK_FACILITY_ICON_TYPES`(내장 대체값)로 계속 동작
4. 아이콘 추가·변경은 **코드 배포 없이 DB 행만 수정**하면 됨(브라우저 새로고침 시 반영, API 캐시 1시간)

## 검증 결과

- 백엔드 재빌드 후 기동, 엔드포인트 직접 호출: **200 + `[]`** (테이블 미생성 상태) — 500으로 떨어지지 않고 의도대로 빈 배열 반환.
- 백엔드 로그에 `relation "qfield.facility_icon" does not exist` 원인과 함께 경고 1줄만 남음.
- 게이트웨이 경유 호출 4회 연속 200 확인.
- Chrome에서 시설물 2,474건 렌더링 확인, 콘솔 오류 0건. 경고는 "아이콘 설정이 비어 있어 내장 기본 아이콘을 사용합니다" 1건(의도된 대체 경로).
- 아이콘 분포: 주차 741, 체육 245, 충전 107, 기본 건물 483, 그 외(강당·식당·전시 등) 898.

## 검증 중 발견한 운영 이슈

백엔드만 재기동하니 Eureka에 죽은 인스턴스가 남아 게이트웨이 요청의 **절반이 500**이었습니다. 리스가 만료될 때까지 1~3분 걸립니다. 죽은 인스턴스를 직접 해제해 해결했고, 대처법을 `docs/dev-environment.md`에 적었습니다.

```
Invoke-WebRequest -Method Delete "http://localhost:8761/eureka/apps/MAPSERVICE-REST/<instanceId>"
```

## 참고

- 커밋·push는 하지 않았습니다.
- DB 스크립트를 실행한 뒤에는 `GET /map/qfield/facility-icons`가 7건을 돌려주는지 확인하고, 프론트를 새로고침해 아이콘이 그대로 나오는지 보면 됩니다.
