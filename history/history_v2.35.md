# history v2.35 — 소개 탭의 전체 영상 섹션 제거(재생 지점을 팝업 하나로 통일)

- **날짜**: 2026-09-28
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.34.md](history_v2.34.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 접속 시 소개 팝업 자동 표시 |
| 로컬 소개 탭 | http://localhost:4000 → 상단 "소개" | 이번에 손본 화면 |
| 운영 지도 | https://sj-lab.co.kr/map/ | |

## 실행한 프롬프트

```
소개탭에서 aboutVideoSection는 굳이 없어도될거같아 위에 프로젝트 소개 영상 보기로 보면되니까
```

## 작업 내용

v2.34에서 소개 탭에 넣었던 전체 영상 섹션을 걷어냈습니다. 같은 탭 히어로 영역에 이미 **"프로젝트 소개 영상 보기"** 버튼(`window.SjIntroModal.open()`)이 있어 팝업에서 요약본 → 전체본으로 볼 수 있으므로, 재생 지점을 팝업 하나로 통일했습니다. 같은 25MB 영상을 두 군데서 내려받을 일도 없어집니다.

| 파일 | 변경 |
|---|---|
| `index.html` | `<section class="about-video-section" id="aboutVideoSection">` 블록 삭제(플레이어 `#aboutFullVideo`, 포스터, 챕터 버튼 4개 `data-seek="5|74|188|354"`). 히어로 → 지표 그리드가 바로 이어짐. `intro-modal.css?v=20260928b`로 캐시 무효화 |
| `js/modules/intro-modal.js` | 챕터 시킹 바인딩 블록(`aboutFullVideo`, `.about-video-chapters [data-seek]`) 제거. 팝업 쪽 요약↔전체 전환 로직(`playFullIntroVideo`)은 그대로 |
| `css/components/intro-modal.css` | 쓰이지 않게 된 `.about-video-section` · `.about-video-head` · `.about-video-frame` · `.about-video-chapters`와 전용 미디어 쿼리 삭제 (8,951B → 7,327B) |
| `docs/ui-conventions.md` | "소개 탭에 전체 영상 섹션" 설명을 **"전체 영상은 팝업 하나에서만 재생 — 플레이어를 다시 추가하지 말 것"** 으로 교체 |
| `videos/README.md` | 표의 `full-demo.mp4` 용도에서 소개 탭 제거, 3번 항목 수정, 교체 시 함께 고칠 대상을 챕터 초 → 배지/버튼 문구로 변경 |
| `README.md` | 소개 탭 설명을 "버튼으로 같은 팝업을 다시 열 수 있다"로 수정 |

`videos/full-poster.jpg`는 팝업이 전체본으로 전환할 때 쓰므로 그대로 둡니다.

## 검증 (로컬, 헤드리스 Chrome + CDP)

정적 서버(4000)에 토큰을 주입해 로그인 게이트를 통과시킨 뒤 확인했습니다.

| 확인 | 결과 |
|---|---|
| 접속 직후 팝업 | 열림, `intro-summary.mp4`, 음소거 자동재생, 배지 "1분 요약", 전체 버튼 보임 |
| "전체 소개 영상 보기 (6분)" | `full-demo.mp4`, 음소거 해제, 재생 중, 배지 "전체 6분", 버튼 숨김 |
| 소개 탭 | `#aboutVideoSection` 없음 · `#aboutFullVideo` 없음 · 챕터 0개 · 탭 안 `<video>` 0개, 지표 카드 4개는 그대로 |
| 소개 탭 "프로젝트 소개 영상 보기" | 팝업 다시 열림 + **요약본부터** 재생(배지 "1분 요약") |
| "1일 동안" 다시 보지 않기 | `localStorage` 저장 + 팝업 닫힘 |
| 콘솔 오류 | 없음 (`node --check js/modules/intro-modal.js` 통과) |

화면은 히어로 → 지표 그리드 → 엔드투엔드 파이프라인으로 자연스럽게 이어지고 빈 공간이 남지 않는 것을 스크린샷으로 확인했습니다.

## 기록 번호 정정

v2.34의 영상 작업을 처음에 `history_v2.26.md`로 저장해, 2026-09-23에 작성된 기존 v2.26(전체 저장소 README 현행화)을 덮어썼습니다. 이번 작업에서 원본 v2.26을 되돌리고 영상 기록을 **v2.34**로 옮겼습니다(공유 웹페이지의 v2.26 패널도 원래 내용으로 복원). 다음 기록은 v2.36부터입니다.

## 남은 작업

- 커밋·push (영상 2개 약 29MB 포함 — 용량이 더 늘면 외부 스토리지/CDN 또는 비공개 YouTube 임베드 검토).
- 운영 배포 후 실제 브라우저에서 팝업 자동재생·전환 확인.
