# history v1.26 — WFS 레이어 최초 로딩 속도 개선(화면 영역 조회 + 응답 압축)

- **날짜**: 2026-09-17
- **영향 저장소**: `mapservice-rest`(백엔드), `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.25.md](history_v1.25.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 프론트엔드 | http://localhost:4000 | 레이어 패널에서 편의점·버스·CCTV·약국·병원·관공서 토글 |
| WFS 화면 영역 조회 예시 | http://localhost:8100/map/busStop-info?bbox=14125000,4510000,14145000,4530000&limit=3000 | 새로 생긴 파라미터 |

## 실행한 프롬프트

```
편의점, 약국, 병원, 관공서, 버스, cctv 레이어가 데이터가 많다보니까
처음 불러올떄 시간이 많이 걸리는데 속도 개선해줄수있어?
```

## 원인 — 화면에 200개 그리려고 전국 20만 개를 받고 있었음

레이어를 켜면 프론트가 전국 데이터를 **통째로** 받은 뒤 브라우저에서 잘라 쓰고 있었습니다.
실제로 화면에 그리는 개수는 줌 레벨에 따라 100~3000개인데, 받는 양은 그와 무관했습니다.
응답 압축도 꺼져 있어 원본 크기 그대로 전송됐습니다.

| 레이어 | 피처 수 | 응답 크기 | DB 쿼리 |
|---|---|---|---|
| 버스정류장 | 203,389 | 84.5 MB | 4,730ms |
| 병원 | 64,498 | 56.6 MB | 2,852ms |
| 편의점 | 48,880 | 16.2 MB | 1,146ms |
| 약국 | 24,533 | 18.2 MB | 877ms |
| CCTV | 11,756 | 6.3 MB | 339ms |
| 관공서 | 9,000 | 4.3 MB | 340ms |

## 변경된 결과물

### 백엔드 `mapservice-rest`

| 파일 | 변경 |
|---|---|
| `mapper/wfs-geojson.xml` | 6개 레이어에 `*ByBbox` 문 추가. 뷰가 아니라 원본 테이블을 조회하되 **뷰와 동일한 중복 제거·properties 구성**을 그대로 옮김 |
| `mapper/WfsMapper.java` | `*ByBbox(minX, minY, maxX, maxY, limit)` 6개 추가 |
| `service/WfsService.java`, `service/impl/WfsServiceImpl.java` | 6개 메서드가 `(double[] bbox, int limit)`를 받도록 변경. `bbox == null`이면 기존 뷰 조회 |
| `controller/WfsController.java` | `bbox`·`limit` 선택 파라미터 추가, bbox 형식 오류 시 400, 조회 로그 추가 |
| `application.yml` | `server.compression` 활성화(`application/json` 포함, 2KB 이상) |

**bbox 조회 SQL의 핵심 두 가지**

1. `st_intersects(geom, st_makeenvelope(..., 3857))` — 각 테이블의 `geom` GIST 인덱스를 탑니다.
2. 상한을 넘을 때 **화면 한쪽에 몰리지 않도록**, bbox 를 `limit`개 격자로 나눠 칸마다 하나씩 먼저 뽑고
   그다음 두 번째를 뽑는 순서로 자릅니다(`row_number() over (partition by 격자 ...)` → `order by rn`).
   개수가 상한 이하면 전부 나오고, 넘으면 화면 전체에 고르게 퍼진 표본이 나옵니다.

```
GET /map/busStop-info?bbox=minX,minY,maxX,maxY&limit=3000   (EPSG:3857, limit 기본 3000·최대 20000)
GET /map/busStop-info                                        (bbox 없으면 기존처럼 전국 전체)
```

`bbox` 없는 호출을 그대로 남겨 둔 것은 **구버전 프론트가 붙어도 동작하게** 하기 위해서입니다.

### 프론트엔드 `sj-lab-mapservice`

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-wfs.js` | 레이어를 켤 때·지도를 옮길 때 **화면 영역만** 요청하도록 재작성 |

- `fetchWfsFeaturesForExtent()` — 화면보다 가로·세로 50% 넓은 영역(`bufferExtent`)을 요청
- `wfsFetchState[layerName]` — `{ extent, zoom, complete }`로 "어디까지 받아왔는지" 기록
- `needsServerFetch()` — ① 아직 안 받음 ② 화면이 받아둔 영역 밖 ③ 상한에 걸린 영역인데 줌인 — 이 세 경우에만 재요청
- `mergeFeaturesIntoCache()` — 피처 id 기준 중복 제거로 캐시에 합침
- `renderLayerFromCache()` — 화면에 그리는 일을 한 곳으로 모음(뷰포트 필터 → 줌별 상한 → `spatialSampling`)
- 기존 `filterRawPointFeaturesByExtent()`·`scheduleBackgroundFeatureCaching()`은 **bbox 를 모르는 예전 백엔드에 붙었을 때의 안전장치**로만 남김

### 문서

| 파일 | 변경 |
|---|---|
| `mapservice-rest/CLAUDE.md` | WFS bbox 조회 패턴과 "뷰 정의가 바뀌면 XML도 함께" 주의 추가 |
| `mapservice-rest/docs/system-architecture.md` | API 계약 표에 `bbox`·`limit` 반영, 두 모드·격자 표본·응답 압축 설명 추가 |
| `sj-lab-mapservice/docs/map-architecture.md` | `loadWfsData()` 설명을 화면 영역 조회 구조로 교체 |

## 검증 결과

### 백엔드 (게이트웨이 8100 경유, 로컬)

| 구분 | 전송량(합계 6개) | 시간(합계) |
|---|---|---|
| 변경 전(전체 조회, 압축 없음) | 203 MB | 29.4초 |
| 변경 후(bbox 3000 + gzip) | **718 KB** | **1.2초** |

전체 조회 경로도 압축이 붙어 203MB → 21.8MB로 줄었습니다(구버전 프론트 대비 효과).

### 브라우저 (실제 Chrome, localhost:4000)

| 확인 | 결과 |
|---|---|
| 레이어 켜고 화면에 표시될 때까지 | 버스정류장 315ms · 병원 544ms · 편의점 266ms |
| 요청 형태 | `?bbox=14058465,4473893,14196173,4562419&limit=1500` (3개 레이어 모두) |
| 받아둔 영역 밖으로 이동 | 3개 레이어 모두 새 bbox 로 추가 조회 |
| 버퍼 안에서 소폭 이동 | 추가 요청 **0건** (캐시 사용) |
| 콘솔 오류 | 0건 |

### 최종 SQL(격자 표본) 재검증 — 완료

작업 도중 개발 DB와 운영 API가 함께 불통이 되어 한 차례 중단됐다가, 복구 후 아래 세 가지를 모두 확인했습니다.

**1. JDBC 파라미터 바인딩 경로** — 게이트웨이(8100) 경유로 6개 레이어 전부 200 응답. 파라미터 타입 추론 오류 없음.

| 레이어 | 피처수 | 전송(gzip) | 원본 | ms |
|---|---|---|---|---|
| 편의점 | 3,000 | 147KB | 1,191KB | 755 |
| 버스정류장 | 1,446 | 70KB | 640KB | 235 |
| CCTV | 0 | - | - | 40 |
| 약국 | 2,171 | 211KB | 1,749KB | 517 |
| 병원 | 3,000 | 310KB | 2,915KB | 1,164 |
| 관공서 | 433 | 25KB | 232KB | 101 |

(합계 764KB / 2.8초 — 첫 호출이라 JIT·커넥션 풀 예열이 포함된 값)

**2. 표본 분포** — 전국 bbox, `limit=900`으로 9개 구역별 개수(위→아래):

```
편의점        27 169  73      버스정류장    24 147  55      병원        34 175  59
              56 253 155                    51 264 171                  57 243 166
              72  81  14                    67 110  11                  68  87  11
```

빈 구역 없이 전 구역에 분포하며, 가운데(수도권)가 많은 것은 시설 자체가 거기 몰려 있기 때문입니다.
단순 `limit`이면 한 구역에 900개가 전부 몰리고, 중심 거리순이면 가운데 한 칸에만 몰립니다.

**3. 브라우저 재확인** — 최종 SQL 기준

| 확인 | 결과 |
|---|---|
| 레이어 켜고 화면에 표시될 때까지 | 버스정류장 460ms · 병원 623ms · 편의점 442ms |
| 받아둔 영역 밖으로 이동 | 3개 레이어 모두 새 bbox 로 추가 조회 |
| 버퍼 안에서 소폭 이동 | 추가 요청 **0건** |
| 화면 표출 | 아이콘이 화면 전체에 고르게 분포(한쪽 쏠림 없음) |
| 콘솔 오류 | 0건 |

## 참고

- CCTV는 서울 도심 bbox 에서 0건이 나옵니다. ITS 도로 CCTV라 도심에 적은 것이며, 전국 조회에서는 정상적으로 나옵니다.
- `image.tag`는 Jenkins가 관리하므로 이번 변경에 포함하지 않았습니다.
