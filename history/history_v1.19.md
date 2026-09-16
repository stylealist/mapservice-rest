# history v1.19 — 음성 관련 항목 라벨 정리

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.18.md](history_v1.18.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 시설물 선택 후 팝업 확인 |

## 실행한 프롬프트

```
현장 특이사항 텍스트화를 적당한 한글명으로 변경해줘
```

## 변경된 결과물

| 키 | 이전 라벨 | 새 라벨 |
|---|---|---|
| `facility_memo_txt` | 현장 특이사항 텍스트화 | **음성 변환 내용** |
| `audio_memo_txt` | 음성내용 | **음성 메모 텍스트** |

요청은 `facility_memo_txt` 하나였지만 `audio_memo_txt`도 함께 바꿨습니다. 새 라벨이 "음성 변환 내용"이 되면서 기존 "음성내용"과 구분이 되지 않기 때문입니다.

팝업에서 이 구간은 이렇게 읽힙니다.

```
현장 특이사항      ▶ (음성 재생)
음성 변환 내용     받아쓴 텍스트
현장 영상          ▶ (영상 재생)
음성 메모 텍스트   -
```

## 검증 결과

Chrome에서 팝업을 열어 라벨 20개가 의도대로 표시되는 것과 콘솔 오류 **0건**을 확인했습니다.

## 참고

- 파일은 `js/modules/map/map-facility.js` 하나만 바뀌었습니다.
- 지도 프론트엔드는 아직 실서버 `/map` 경로에서 서비스되지 않는 상태입니다(v1.10 확인).
