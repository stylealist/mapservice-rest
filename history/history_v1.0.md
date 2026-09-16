# history v1.0 — 왼쪽 패널 시설물 탭(QField 시설물 데이터 표출)

- **날짜**: 2026-09-15 이전 작업 (2026-09-16 소급 기록)
- **영향 저장소**: `mapservice-rest`(백엔드), `sj-lab-mapservice`(프론트엔드)
- **상태**: 두 저장소 모두 **아직 커밋되지 않은 작업 트리 변경**
- **다음 버전**: [history_v1.1.md](history_v1.1.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 지도 프론트엔드(시설물 탭) | http://localhost:4000 | 왼쪽 패널 기본 탭 |
| 시설물 목록 API | http://localhost:8100/map/qfield/facilities | 게이트웨이 경유 |
| 시도 목록 API | http://localhost:8100/map/admin-area/sido | 게이트웨이 경유 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 브라우저로 바로 열기 |
| 공유 웹사이트(정적 서버) | http://localhost:4100 | `python -m http.server 4100 --directory history/web` |

## 실행한 프롬프트

기능 구현 자체는 이전 세션에서 진행되어 프롬프트 원문이 남아 있지 않습니다. 이 문서는 아래 요청으로 소급 작성했습니다.

```
/orchestration 이전에 생성한 왼쪽탭에 데이터 표출하는 부분에 대해서 history 작성해줘
```

## 기능 개요

지도 왼쪽 레이어 패널에 **시설물 탭**을 추가해, 행정구역(시·도 → 시·군·구 → 읍·면·동)을 좁혀가며 QField 시설물을 목록과 지도에 함께 표출하는 기능입니다. 목록 항목을 선택하면 지도가 해당 시설물로 이동하고 팝업으로 상세 정보를 보여줍니다.

데이터 흐름: `qfield.facility_total_view` + `public.g_sido/g_sgg/g_emd` → mapservice-rest(MyBatis, DB에서 GeoJSON 조립) → API Gateway `/map/**` → 프론트 `map-facility.js` → 왼쪽 패널 목록 + OpenLayers 레이어.

## 변경된 결과물

### 백엔드 (`mapservice-rest`, 신규 5개 파일)

| 파일 | 줄 수 | 역할 |
|---|---|---|
| `controller/QfieldFacilityController.java` | 96 | 엔드포인트 5개. 코드 자릿수 검증 실패 시 400, 시설물 미존재 시 404, `Cache-Control` 지정(시설물 60초, 행정구역 3600초) |
| `service/QfieldFacilityService.java` | 8 | 서비스 인터페이스 |
| `service/impl/QfieldFacilityServiceImpl.java` | 43 | 매퍼 위임 |
| `mapper/QfieldFacilityMapper.java` | 11 | MyBatis 매퍼 인터페이스(자바 쪽 SQL 없음) |
| `resources/mapper/qfield-facility.xml` | 146 | `getFacilities`, `getFacilityDetail`, `getSidoList`, `getSggList`, `getEmdList` |

DB 뷰를 새로 만들 수 없는 환경이라, 기존 WFS 패턴(뷰의 `geojson` 컬럼 select) 대신 **XML SQL에서 `json_build_object`·`json_agg`·`ST_AsGeoJSON`으로 GeoJSON을 직접 조립**하는 방식을 사용했습니다.

주요 쿼리 설계:
- 시설물(EPSG:3857 점)과 행정구역 경계(EPSG:4326)를 `LEFT JOIN LATERAL` + `ST_Intersects(e.geom, ST_Transform(f.geom, 4326))`(LIMIT 1)로 연결해 `g_emd`의 GIST 인덱스를 활용.
- 행정구역 코드의 접두어 계층을 이용해 `emd_cd LIKE code || '%'` 단일 조건으로 필터링(`g_sido` 폴리곤 직접 조인·`ST_MakeValid` 회피).
- 행정구역 extent는 폴리곤 전체가 아니라 `ST_Transform(ST_Envelope(geom), 3857)`로 BBOX만 변환.

### 프론트엔드 (`sj-lab-mapservice`)

| 파일 | 변경 | 내용 |
|---|---|---|
| `js/modules/map/map-facility.js` | 신규 755줄 | 시설물 모듈 전체. 함수 14개(`initializeFacilityModule`, `bindAdminAreaSelects`, `loadFacilitySidoList/SggList/EmdList`, `loadFacilities`, `selectFacility`, `showFacilityDetail`, `closeFacilityPopup`, `facilityStyleFunction`, `createPopupElement`, `fitMapExtent`, `getFacilityLayer`, `getFacilitySource`) |
| `index.html` | +46/-2 | 시설물 탭 마크업(연쇄 select 3개, 건수 표시, 로딩·빈결과·오류 상태, 목록 `ul`). 기본 활성 탭을 길찾기 → 시설물로 변경, 패널 제목 기본값도 "시설물" |
| `css/components/layer-panel.css` | +294줄 | `.facility-*` 클래스 일습(select, 목록, 항목 선택 상태, 배지, 스피너, 팝업) |
| `js/modules/map/map.js` | +35 | 시설물 모듈 import·초기화 호출·`window.*` 전역 등록(8개)·export |
| `js/modules/ui.js` | +22 | 탭 전환 시 `#panelTitle` 문구 매핑, 패널 접기/펼치기 후 `updateSize()` 호출(320ms 뒤) |
| `docs/map-architecture.md` | +15 | 시설물 레이어 규격(zIndex 1010, 이벤트 ID, 요청 취소 규칙, API 목록) |
| `docs/ui-conventions.md` | +5 | 시설물 탭·연쇄 select 컨벤션 |

### API 계약

| 엔드포인트 | 응답 |
|---|---|
| `GET /map/qfield/facilities?sidoCd=&sggCd=&emdCd=` | 시설물 GeoJSON FeatureCollection |
| `GET /map/qfield/facilities/{totalId}` | 시설물 상세 Feature(`sido_nm`·`sgg_nm`·`emd_nm` 포함) |
| `GET /map/admin-area/sido` | 시도 목록 + extent |
| `GET /map/admin-area/sgg?sidoCd=` | 시군구 목록 + extent |
| `GET /map/admin-area/emd?sggCd=` | 읍면동 목록 + extent |

## 주요 설계 결정

- **레이어 우선순위**: 시설물 레이어 `zIndex: 1010` — 기존 WFS/WMS(1000) 위에 표시.
- **건수 일치**: `declutter: false`, `updateWhileAnimating/Interacting: false` — 목록 건수와 지도 표출 건수를 일치시키기 위함.
- **요청 경쟁 방지**: `AbortController` + 요청 순번(`facilityRequestSeq`)으로 이전 요청을 취소하고 최신 응답만 반영. 취소로 발생한 `AbortError`는 오류 UI를 띄우지 않음.
- **이벤트 충돌 회피**: 클릭 `facility-click-layer`, 호버 `facility-pointer-move`로 별도 ID 등록. 기존 `wfs-general-click`과 분리하고, 시설물 피처를 벗어나면 커서를 즉시 복원해 커서 고착 방지.
- **대량 렌더링**: 약 2,474건을 `DocumentFragment`로 한 번에 삽입, XSS 방지를 위해 `textContent` 사용.
- **상태 표시**: 인라인 `display:none` 대신 `.facility-state-message.hidden` CSS 클래스로 제어.

## 검증 결과 (2026-09-16, 백엔드 직접 호출)

| 호출 | 결과 |
|---|---|
| `/map/qfield/facilities` | 200, 2,474건, 약 1.5초 |
| `/map/admin-area/sido` | 200, 107ms |
| `/map/admin-area/sgg?sidoCd=11` | 200, 48ms |
| `/map/admin-area/emd?sggCd=11110` | 200, 47ms |
| `/map/qfield/facilities/__none__` | 404 (의도대로) |
| `/map/admin-area/sgg?sidoCd=abc` | 400 (의도대로) |

프론트에서 지도·시설물 모듈 초기화까지는 확인했으나, **브라우저에서 게이트웨이를 거친 목록 표출·행정구역 필터·팝업 동작은 아직 끝까지 확인하지 못했습니다**(v1.1의 미해결 과제로 이어짐).

## 참고

- 두 저장소 모두 이 작업이 커밋되지 않은 상태입니다. 커밋 시 백엔드 5개 파일과 프론트 7개 파일(신규 `map-facility.js` 포함)을 함께 묶는 것이 좋습니다.
- 프론트 `docs/map-architecture.md`에 적힌 `readFeatures()`의 `featureProjection` 주의사항(뷰 좌표계 3857 그대로 사용)은 이 시설물 레이어에도 그대로 적용됩니다.
