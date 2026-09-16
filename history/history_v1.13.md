# history v1.13 — 상세 팝업 일시 표기를 `YYYY-MM-DD HH:MM` 으로 정리

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.12.md](history_v1.12.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 시설물 선택 후 팝업 확인 |

## 실행한 프롬프트

```
등록 일 시 수정 일시가 yyyy-mm-dd ss:mm 으로 나오면 좋겠어
```

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-facility.js` | `formatFacilityDateTime()` 추가, `DETAIL_FIELD_CONFIG`에 `isDateTime` 플래그, 상세 렌더링에서 적용 |

### 표기 변화

| 항목 | 이전 | 이후 |
|---|---|---|
| 등록일시 | `2026-08-28T05:12:42.635018` | `2026-08-28 05:12` |
| 수정일시 | `2026-09-16T04:06:22.637368` | `2026-09-16 04:06` |

### 구현 메모

- 백엔드가 주는 값은 **타임존이 없는 로컬 시각 문자열**입니다. `new Date()`로 파싱하면 브라우저 타임존만큼 어긋나므로, 정규식으로 `YYYY-MM-DD`와 `HH:MM`만 잘라 씁니다.
- 형식이 예상과 다르면 원본 문자열을 그대로 보여줍니다(값이 사라지지 않게).
- 요청은 등록·수정일시였지만 **점검일시(`inspected_at`)에도 같은 플래그를 적용**했습니다. 같은 성격의 일시 필드라 표기가 갈리면 어색하기 때문입니다.
- 앞으로 일시 필드가 추가되면 `DETAIL_FIELD_CONFIG`에 `isDateTime: true`만 붙이면 됩니다.

## 검증 결과

Chrome에서 시설물 팝업을 열어 확인했습니다.

- 등록일시 `2026-08-28 05:12`, 수정일시 `2026-09-16 04:06`
- 콘솔 오류 **0건**

## 참고

- 프롬프트의 `ss:mm`은 시:분을 뜻하는 것으로 보고 **`HH:MM`(시:분)** 으로 적용했습니다. 초 단위까지 필요하면 알려 주세요.
- 지도 프론트엔드는 아직 실서버 `/map` 경로에서 서비스되지 않는 상태입니다(v1.10 확인).
