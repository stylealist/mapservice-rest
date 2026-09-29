# history v2.46 — `sj-lab` README에 개발 변경 로그 행 추가 (v2.45의 남은 작업 완료)

- **날짜**: 2026-09-29
- **영향 저장소**: `sj-lab`(GitHub, 로컬 clone 없음), `mapservice-rest`(기록·문서)
- **이전 버전**: [history_v2.45.md](history_v2.45.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 이번에 README에 링크한 페이지 |
| `sj-lab` README | https://github.com/stylealist/sj-lab | Live Demo 표 마지막 행 |
| 운영 지도 | https://sj-lab.co.kr/map/ | 소개·저장소 탭에도 같은 링크 |

## 실행한 프롬프트

```
이어서 진행해줘
```

(직전 v2.45에서 "`sj-lab` README 반영 여부 확인"을 남겨 둔 상태였습니다.)

## 작업 내용

### 1. `sj-lab` README — Live Demo 표에 한 행 추가

"1. 서비스 접속 및 실서비스 체험 안내(Live Demo)" 표 마지막(QFieldCloud 다음)에 넣었습니다.

```markdown
| **기록** | **개발 변경 로그 (v1.0 ~ 현재)** | [https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9](https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9) | 하네스(Harness) 엔지니어링으로 개발하며 버전마다 남긴 작업 기록 — 무엇을 왜 바꿨는지, 어떻게 검증했는지 |
```

- 이 저장소는 **로컬 clone이 없어** 임시 디렉터리에 `git clone --depth 1` → README 교체 → 커밋 → push 방식으로 처리했고, 작업 뒤 임시 디렉터리는 정리했습니다.
- 커밋: `432a981..0039991` (`docs: Live Demo 표에 개발 변경 로그 페이지 링크 추가`).
- **검증**: GitHub API로 `main`의 README를 다시 읽어 행이 들어간 것을 확인했습니다(파일 28,040B → 28,351B, 표 행 21개). `raw.githubusercontent.com`은 CDN 캐시 때문에 잠시 옛 내용을 돌려주므로 API 조회로 확인했습니다.

### 2. 문서

- `docs/dev-environment.md` — **로컬 clone이 없는 저장소(`stylealist/sj-lab`)** 항목을 추가하고, 임시 clone → 수정 → push 절차와 "Live Demo 표 주소가 바뀌면 지도 프론트의 저장소·문의 탭과 함께 확인할 것"을 기록.

## 현재 링크가 걸린 곳 (같은 페이지 3곳)

| 위치 | 표시 |
|---|---|
| 지도 소개 탭 히어로 | "개발 변경 로그 보기" 버튼 |
| 지도 저장소·문의 탭 | 저장소 목록 위 강조 링크 "개발 변경 로그 (v1.0 ~ 현재)" |
| `sj-lab` README | Live Demo 표 "기록 · 개발 변경 로그 (v1.0 ~ 현재)" |

주소가 바뀌면 **세 곳을 함께** 고쳐야 합니다(`docs/ui-conventions.md`·`docs/dev-environment.md`에 기록).

## 남은 작업

- 커밋·push (v2.45 프론트 변경 + 이번 문서).
