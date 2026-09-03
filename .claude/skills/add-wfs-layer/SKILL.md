---
name: add-wfs-layer
description: mapservice-rest에 새로운 WFS GeoJSON 지도 레이어 엔드포인트를 스캐폴딩합니다(예: "주차장 레이어 추가해줘", "add a parking-lot layer"). 사용자가 기존 편의점/버스정류장/CCTV/약국/병원/관공서 레이어와 같은 패턴으로 PostGIS 뷰를 GeoJSON REST 엔드포인트로 노출하고 싶을 때 사용하세요.
---

# WFS 레이어 추가

이 저장소는 각 지도 레이어를 PostGIS 뷰에서 이미 완성된 `geojson` 텍스트 컬럼을 select하는 방식으로 노출합니다. 기존 레이어(편의점, 버스정류장, CCTV, 약국, 병원, 관공서) 전부가 동일한 4파일 패턴을 따르며, 이 스킬은 새 레이어도 같은 방식으로 만들어 줍니다. GeoJSON 조립은 항상 DB 뷰에서 이루어지고 자바에서는 절대 하지 않습니다 — 이 스킬도 GeoJSON을 조립하는 코드는 작성하지 않고, 이미 존재하거나 곧 만들어질 DB 뷰를 호출하는 배관(plumbing) 코드만 작성합니다.

## 필요한 입력

사용자에게 다음을 물어보거나(또는 요청에서 추론):
- **레이어 이름**: 메서드 이름용 camelCase(예: `parkingLot`)와 URL용 kebab/소문자 형태(예: `parking-lot`).
- **DB 뷰 이름**: 저장소 관례를 따르는 `map.v_<layer>_geojson`(예: `map.v_parking_lot_geojson`). 아직 뷰가 존재하지 않는다면 최종 요약에서 반드시 명시하세요 — 이 스킬은 앱 쪽 배관만 연결할 뿐 DB 뷰를 만들지 않습니다.

## 절차

기존 레이어(예: `pharmacyInfo`/`pharmacy-info`)를 그대로 본떠서 아래 순서로 4개 파일을 수정하세요:

1. **`src/main/java/com/example/mapservice/mapper/WfsMapper.java`**
   메서드 선언 추가: `String <layerName>();`

2. **`src/main/resources/mapper/wfs-geojson.xml`**
   기존 `<mapper>` 엘리먼트 안에 `<select id="<layerName>" resultType="String">select geojson from map.v_<layer>_geojson</select>` 항목 추가.

3. **`src/main/java/com/example/mapservice/service/WfsService.java`**와 **`.../service/impl/WfsServiceImpl.java`**
   인터페이스 메서드와 `mapper.<layerName>()`에 위임하는 구현체를 추가하세요. 기존 메서드들의 스타일을 따르세요(일부는 `throws Exception`을 선언하고 일부는 안 하는데, 통일할 필요 없이 가장 가까운 기존 메서드를 그대로 따르면 됩니다).

4. **`src/main/java/com/example/mapservice/controller/WfsController.java`**
   `@GetMapping("/<layer-kebab>-info")` 메서드를 추가해 서비스를 호출하고, `Exception`을 catch하며(기존 메서드들의 삼키고 로그만 남기는 패턴을 그대로 따르세요 — 이는 CLAUDE.md에 명시된 저장소의 기존 관례이지, 스캐폴딩하면서 "고쳐야 할" 대상이 아닙니다), 다른 엔드포인트와 똑같이 `Cache-Control: public, max-age=300` 헤더와 geojson 문자열 바디를 담은 `ResponseEntity.ok()`를 반환하세요.

## 스캐폴딩 이후

- 4개 파일에서 레이어 이름이 일관되게 사용됐는지 확인하세요(이것이 정확히 `.claude/agents/reviewer.md`가 체크하는 항목입니다).
- DB 뷰(`map.v_<layer>_geojson`)가 이미 존재하는지, 아직 만들어야 하는지를 사용자에게 명확히 알려주세요 — 뷰가 없어도 엔드포인트는 컴파일되고 응답까지 하지만 런타임에 오류가 납니다.
- 요청받지 않은 테스트, Swagger 어노테이션, 다른 레이어를 추가로 만들지 마세요 — 기존 6개 레이어 어디에도 그런 것들은 없습니다.
