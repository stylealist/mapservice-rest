# history v2.33 — sj-lab-mapservice 프로젝트 설명 영상 팝업 및 기간별(1·7·30일) 다시 보지 않기 구현

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`, `mapservice-rest`
- **이전 버전**: [history_v2.32.md](history_v2.32.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 플랫폼 마스터 저장소 | https://github.com/stylealist/sj-lab | 전체 아키텍처 및 11개 서브 프로젝트 맵 |
| 시설물 관리 프론트 저장소 | https://github.com/stylealist/sj-lab-mapservice | 설명 영상 팝업이 적용된 GIS 웹 정적 SPA |
| 시설물 관리 실서비스 | https://sj-lab.co.kr/map/ | 접속 시 인트로 영상 모달 팝업 표출 |
| 통합 랜딩 허브 | https://sj-lab.co.kr | 멀티 서비스 클라우드 허브 포털 |

## 실행한 프롬프트

```
sj-lab-mapservice에 접속할떄 프로젝트에 대한 설명영상 팝업이 나오도록 해줬으면 좋겠고 자세히보기 버튼을 누르면 소개탭으로 이동하도록해줘 그리고 1일,7일,30일 팝업안보기 기능도 구현해줘 바로 commit push하지말고 개발후에 알려줘
```

## 작업 내용 및 개선 사항

### 1. 프로젝트 설명 영상 인트로 팝업 모달 구현
- **신규 모듈**: `js/modules/intro-modal.js`
- **신규 스타일시트**: `css/components/intro-modal.css`
- **신규 미디어 디렉터리**: `videos/` (및 `videos/README.md`)
- **접속 시 자동 표출**: 사이트 접속 600ms 후 부드러운 스케일업 & 페이드인 애니메이션과 함께 모달 오픈.
- **영상 플레이어 & 폴백(Fallback) 방어 체계**:
  - 16:9 비율의 모던 비디오 플레이어 컨테이너(`intro-video-wrapper`) 배치.
  - 기본 영상 소스: `videos/sj-lab-intro.mp4`.
  - 미디어 파일 미존재 또는 로드 실패 시, `error` 이벤트를 감지하여 자동으로 시스템 시연 핵심 브리핑 카드(`introVideoFallback`)를 노출하여 UI 깨짐 방지.
  - 비디오 포스터로 지형 맵 썸네일(`images/mapImg/baseMap.png`) 기본 적용.

### 2. "자세히 보기" 연동
- 모달 내 "자세히 보기" 버튼 클릭 시:
  - 현재 재생 중인 비디오 정지(`video.pause()`).
  - 모달 닫기 처리.
  - 상단 헤더의 '소개' 네비게이션 버튼(`.nav-btn[data-page="about"]`)을 자동 트리거하여 소개 탭으로 매끄럽게 전환.

### 3. 기간별(1일 / 7일 / 30일) 다시 보지 않기 제어
- 하단 푸터에 1클릭 칩 버튼 그룹 제공:
  - `[1일 동안]`: 24시간 동안 팝업 억제 (`Date.now() + 1일`)
  - `[7일 동안]`: 7일(1주일) 동안 팝업 억제 (`Date.now() + 7일`)
  - `[30일 동안]`: 30일(1개월) 동안 팝업 억제 (`Date.now() + 30일`)
  - `[닫기]`: 기간 저장 없이 이번 세션만 닫기
- `localStorage`의 `SJ_MAP_INTRO_HIDE_UNTIL` 키에 만료 시점 타임스탬프를 보관하고, 접속 시 현재 시간과 비교하여 팝업 노출 여부를 판별.
- 개발 및 디버그용 인터페이스(`window.SjIntroModal.resetHideStatus()`, `open()`, `close()`) 제공.

### 4. 소개·연락처 페이지 대화면 확장 및 스크롤바 위치 개선
- **기존 문제**: `.info-container`에 `max-width: 1080px; margin: 0 auto; overflow-y: auto;`가 걸려 있어 스크롤바가 대화면 모니터에서 화면 중앙 우측에 어색하게 떠 있던 문제 해결.
- **구조 개선**:
  - 스크롤 래퍼(`.info-container`)를 `width: 100%`로 확장하여 **스크롤바(사이드바)가 브라우저 맨 우측 끝에 정확히 밀착**되도록 교정.
  - 내부 콘텐츠 래퍼(`.info-inner`)를 신설하여 `max-width: 1480px` 및 유연한 패딩을 적용함으로써 대화면 공간을 시원하고 여유롭게 활용.
  - 소개 탭 히어로 섹션에 **[▶ 프로젝트 소개 영상 보기]** 버튼을 추가하여, 팝업 '안 보기' 상태에서도 언제든 원클릭으로 영상을 다시 열람할 수 있도록 편의성 제공.

### 5. 형상 관리 지침 준수
- 사용자 요청에 따라 **커밋 및 원격 푸시를 즉시 진행하지 않고**, 로컬 개발 및 문법 검증 완료 상태에서 대기.
