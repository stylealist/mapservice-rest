# history v1.21 — 오디오 플레이어가 보이지 않던 오류 수정

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.20.md](history_v1.20.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 첨부가 있는 시설물 팝업에서 확인 |

## 실행한 프롬프트

```
audio_memo - 현장 특이사항 가 오디오인데 오디오는 듣는게 없어
```

## 원인

측정해 보니 **오디오가 재생은 되고 있었지만 화면에 보이지 않았습니다.**

| 값 | 수정 전 | 수정 후 |
|---|---|---|
| 오디오 요소 폭 | **0px** | **386px** |
| 오디오 요소 높이 | 34px | 36px |
| `display` | `inline` | `block` |
| 재생 상태 | `readyState 4`, 재생 중(9.7초) | 동일 |

`.facility-detail-row`에 있던 `align-items: flex-start`가 전체 폭 행(`.facility-detail-row-block`)에도 그대로 적용되어, 세로 배치에서 **자식의 폭이 내용 크기로 줄어들었습니다.** 사진·영상은 이미지·비디오 자체에 고유 크기가 있어 티가 나지 않았지만, `width: 100%`로만 지정된 오디오는 폭이 0이 되어 컨트롤이 렌더링되지 않았습니다.

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `css/components/layer-panel.css` | 전체 폭 행에 `align-items: stretch`와 값 영역 `width: 100%` 지정, 오디오·비디오에 `display: block` 및 오디오 최소 폭(240px) 추가 |

## 검증 결과

- 오디오: 폭 **386px**, 높이 36px, 컨트롤 표시, 재생 확인(9.7초)
- 영상: 폭 386px, 높이 220px, 플레이어 정상 표시(`0:00 / 0:02`)
- 콘솔 오류 **0건**

## 참고

- 지도 프론트엔드는 아직 실서버 `/map` 경로에서 서비스되지 않는 상태입니다(v1.10 확인).
- 운영 배포 전 `QFIELD_USERNAME`/`QFIELD_PASSWORD` Secret 주입이 필요합니다(v1.20).
