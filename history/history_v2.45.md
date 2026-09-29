# history v2.45 — 개발 변경 로그 페이지 링크를 소개·저장소 탭에 추가

- **날짜**: 2026-09-28
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록) / 확인 대기: `sj-lab`(GitHub README)
- **이전 버전**: [history_v2.44.md](history_v2.44.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | **이번에 사이트에서 링크한 페이지** |
| 로컬 소개 탭 | http://localhost:4000 → "소개" | 히어로의 "개발 변경 로그 보기" |
| 로컬 저장소·문의 탭 | http://localhost:4000 → "저장소 · 문의" | 저장소 카드 위 강조 링크 |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 후 확인 |

## 실행한 프롬프트

```
sj-lab github하고 소개, 저장소 문의에 https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9로 추가해줘
해당 부분은 하네스 엔지니어링으로 개발을 진행하면서 변경 로그를 남겨둔 페이지야
```

## 작업 내용

### 1. 소개 탭 — 히어로에 버튼 한 개 추가

"프로젝트 소개 영상 보기" 옆에 **"개발 변경 로그 보기"**(`.info-log-link`)를 같은 모양으로 붙였습니다. 새 탭으로 열리고(`target="_blank" rel="noopener noreferrer"`), `a` 태그라 밑줄이 생기지 않게 CSS를 맞췄습니다.

### 2. 저장소·문의 탭 — 저장소 카드 위 강조 링크

GitHub 저장소 목록 **위**에 눈에 띄는 링크 상자(`.channel-highlight-link`)를 두었습니다.

- 제목: **개발 변경 로그 (v1.0 ~ 현재)**
- 설명: 하네스 엔지니어링으로 개발하면서 버전마다 남긴 작업 기록 — 무엇을 왜 바꿨는지, 어떻게 확인했는지

저장소 13개 목록은 그 아래 그대로 두어, "코드 → GitHub / 과정 → 변경 로그"로 읽히게 했습니다.

### 3. 문서

- `docs/ui-conventions.md` — 변경 로그 링크가 **두 곳**에 있으니 주소가 바뀌면 함께 고칠 것, 이 주소는 `mapservice-rest/history/web/artifact.html`의 발행 페이지라는 점을 기록.
- `README.md` — 저장소·문의 탭 설명에 변경 로그 페이지 추가.
- `index.html` — `info-pages.css?v=20260928c`.

## 검증 (로컬, 헤드리스 Chrome + CDP, 1440×1000)

| 확인 | 결과 |
|---|---|
| 소개 탭 | 링크 존재, 주소 정확, `target="_blank"`, 글자 "개발 변경 로그 보기", 영상 버튼과 **같은 줄**, 밑줄 없음 |
| 저장소·문의 탭 | 강조 링크 존재, 주소 정확, 제목·설명 표출, **저장소 목록보다 위**, 저장소 13개 유지 |
| 콘솔 오류 | 없음 |

## 남은 작업 — `sj-lab` 저장소 README (사용자 확인 대기)

`stylealist/sj-lab`은 로컬에 clone돼 있지 않고 `docs/dev-environment.md`의 로컬 저장소 표에도 없어, **원격 커밋 전에 확인을 받기로** 했습니다. README의 "1. 서비스 접속 및 실서비스 체험 안내 (Live Demo)" 표 마지막에 아래 한 줄을 더할 계획입니다.

```markdown
| **기록** | **개발 변경 로그 (v1.0 ~ 현재)** | [https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9](https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9) | 하네스 엔지니어링으로 개발하며 버전마다 남긴 작업 기록 — 변경 내용·이유·검증 방법 |
```

## 참고

- 이 아티팩트는 현재 **"링크가 있는 누구나" 공개** 상태입니다(2026-09-28 발행 결과에서 확인). 사이트에 링크를 거는 만큼 공개 상태가 유지되어야 하고, 반대로 비공개로 바꾸면 사이트의 두 링크가 열리지 않습니다.

## 남은 작업

- 커밋·push.
- `sj-lab` README 반영 여부 확인.
