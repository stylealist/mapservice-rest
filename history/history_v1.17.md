# history v1.17 — 상세 팝업에 점검 결과·사진·음성·영상 추가

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.16.md](history_v1.16.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 시설물 선택 후 팝업 확인 |

## 실행한 프롬프트

```
facility_condition - 시설물 상태
repair_required_yn - 보수 필요 여부
facility_memo - 시설물 특이사항
inspected_at - 점검 일시
(photo_1 photo_2 photo_3 photo_4 photo_5) - 시설물사진 -> 하나의 row에 넘기면서 볼 수 있도록
audio_memo - 현장 특이사항
facility_memo_txt - 현장 특이사항 텍스트화
video - 현장 영상 해당 정보들도 상세 팝업에 추가해줘
```

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `js/modules/map/map-facility.js` | 필드 라벨 정리, 코드값 한글 변환(`FACILITY_VALUE_MAPS`), 사진 갤러리(`createFacilityPhotoGallery`), 음성·영상 플레이어 렌더링 |
| `css/components/layer-panel.css` | 갤러리·음성·영상 스타일, 전체 폭 행(`.facility-detail-row-block`), 라벨 폭 조정 |

### 추가·정리된 항목

| 키 | 라벨 | 표시 방식 |
|---|---|---|
| `facility_condition` | 시설물 상태 | 코드 → 한글(정상 / 경미한 파손 / 파손·고장 / 철거됨) |
| `repair_required_yn` | 보수 필요 여부 | 코드 → 한글(정비요청 / 양호) |
| `facility_memo` | 시설물 특이사항 | 텍스트 |
| `inspected_at` | 점검 일시 | `YYYY-MM-DD HH:MM` |
| `photo_1`~`photo_5` | 시설물 사진 | **한 줄 갤러리** — 좌우 버튼으로 넘기고 `1 / 3` 번호 표시, "원본" 링크로 새 창 열기 |
| `audio_memo` | 현장 특이사항 | 오디오 플레이어(재생 컨트롤) |
| `facility_memo_txt` | 현장 특이사항 텍스트화 | 텍스트 |
| `video` | 현장 영상 | 비디오 플레이어 |

### 코드값 한글 변환의 근거

상태 코드는 임의로 정하지 않고 **현장조사 앱 소스**(`infra-manage-app/src/core/utils/projectutils.cpp`)의 ValueMap을 그대로 옮겼습니다. v1.16에서 그 저장소를 작업 범위에 넣은 덕분에 확인할 수 있었습니다.

```
facility_condition : NORMAL=정상, MINOR_DAMAGE=경미한 파손, BROKEN=파손 / 고장, DESTROYED=철거됨
repair_required_yn : Y=정비요청, N=양호
```

표에 없는 값이 오면 원본을 그대로 보여줍니다. **앱에서 선택지가 바뀌면 `FACILITY_VALUE_MAPS`도 함께 고쳐야 합니다.**

### 구현 메모

- 사진은 `photo_1` 위치에 갤러리 하나로 묶고 나머지 키는 숨겼습니다. `photo_1`이 비어 있고 `photo_3`만 있어도 표시되도록 별도 판정을 넣었습니다.
- 사진·음성·영상 행은 라벨 아래 전체 폭을 쓰도록 `.facility-detail-row-block`을 적용했습니다.
- 상태값이 본문 행으로 나오므로 **헤더 배지는 종류와 "보수 필요"(경고)만** 남겼습니다. 중복 표시를 피하기 위함입니다.

## 검증 결과

| 확인 | 결과 |
|---|---|
| 실제 데이터(`FACIL_T1_329`) | 시설물 상태 **경미한 파손**, 보수 필요 여부 **정비요청**, 점검 일시 `2026-08-24 16:59` |
| 사진 갤러리 | 번호 `1 / 3` 표시, 다음 버튼 클릭 시 `2 / 3`로 이동하고 이미지 교체 확인 |
| 음성·영상 | 각각 오디오·비디오 플레이어로 렌더링 |
| 텍스트 항목 | 시설물 특이사항·현장 특이사항 텍스트화 정상 표시 |
| 콘솔 오류 | **0건** |

## 참고

- **DB에는 아직 사진·음성·영상 데이터가 한 건도 없습니다**(사진 0, 음성 0, 영상 0 / 특이사항 1건, 점검일시 3건). 그래서 미디어 표시는 응답을 가로채 시험값으로 검증했습니다. 실제 데이터가 들어오면 그대로 동작합니다.
- 지도 프론트엔드는 아직 실서버 `/map` 경로에서 서비스되지 않는 상태입니다(v1.10 확인).
