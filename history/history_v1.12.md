# history v1.12 — 상세 팝업에서 내부 관리용 항목 숨김

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.11.md](history_v1.11.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 시설물 선택 후 팝업 확인 |

## 실행한 프롬프트

```
상세 정보 팝업에서 emd_cd, use_yn, sido_cd, origin_id, total_seq, source_fid, source_table은 나오지않도록 수정해줘
```

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-facility.js` | `INTERNAL_DETAIL_KEYS` 상수 추가, 상세 팝업의 "그 밖의 항목" 렌더링에서 제외 |

상세 팝업은 정의된 필드(`DETAIL_FIELD_CONFIG`)를 먼저 그리고, 그 외 응답 프로퍼티를 키 이름 그대로 이어서 보여주는 구조입니다. 그래서 행정구역 코드와 QField 원본 테이블 식별자가 그대로 노출되고 있었습니다.

숨긴 키 8개:

| 키 | 숨긴 이유 |
|---|---|
| `sido_cd`, `sgg_cd`, `emd_cd` | 이름(`시도`·`시군구`·`읍면동`)으로 이미 표시됨 |
| `use_yn` | 내부 사용 여부 플래그 |
| `origin_id`, `total_seq`, `source_fid`, `source_table` | QField 원본 테이블 식별자 — 사용자에게 의미 없음 |

요청 목록에 없던 `sgg_cd`도 함께 숨겼습니다. 같은 행정구역 코드인데 이것만 남으면 어색하기 때문입니다. 다시 보이게 하려면 `INTERNAL_DETAIL_KEYS`에서 빼면 됩니다.

## 검증 결과

Chrome에서 시설물(`생명윤리정책전문도서관`)을 선택해 확인했습니다.

- 팝업에 남은 항목: **기관명 · 도로명주소 · 지번주소 · 시도 · 시군구 · 읍면동 · 담당부서 · 담당자 · 전화번호 · 등록일시 · 수정일시**
- 숨기기로 한 8개 키: **모두 미표시**
- 콘솔 오류: **0건**

## 참고

- 새 필드가 응답에 추가되면 기존처럼 팝업 하단에 키 이름 그대로 노출됩니다. 노출하고 싶지 않으면 `INTERNAL_DETAIL_KEYS`에, 한글 라벨로 보여주려면 `DETAIL_FIELD_CONFIG`에 추가하세요.
- 지도 프론트엔드는 아직 실서버 `/map` 경로에서 서비스되지 않는 상태입니다(v1.10 확인).
