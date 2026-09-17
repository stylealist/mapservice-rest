# history v1.32 — 작업 로그 페이지의 탭이 15개를 넘던 오류 수정

- **날짜**: 2026-09-17
- **영향 저장소**: `mapservice-rest`(history 공유 페이지)
- **이전 버전**: [history_v1.31.md](history_v1.31.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 상단 탭: v1.0 – v1.14 / v1.15 – v1.29 / v1.30 – v1.32 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 발행 원본(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/artifact.html](file:///C:/developer/workspace/mapservice-rest/history/web/artifact.html) | |

## 실행한 프롬프트

```
로그페이지가 한페이지에 15개 넘게 나오는 오류가 있어
```

## 원인

탭 묶음이 마크업에 고정되어 있었고, v1.28부터 새 버전 칩을 두 번째 탭(`vgroup-1`) 끝에 계속 붙이면서
탭 이름만 `v1.15 – v1.31`로 늘려 **한 탭에 17개**가 들어갔습니다. 규칙(`CLAUDE.md`: 한 탭에 15개)을 사람이 매번 지켜야 하는 구조였습니다.

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `history/web/artifact.html` | 마크업을 `v1.0 – v1.14` / `v1.15 – v1.29` / `v1.30 – v1.32` 세 탭으로 분리. 스크립트에 `regroupChips()` 추가 — 로드 시 칩을 순서대로 `chipsPerGroup`(15)개씩 다시 묶고 탭 이름도 자동 생성. v1.32 패널 추가 |
| `history/web/index.html` | v1.32 카드 추가 |
| `CLAUDE.md` | 탭 규칙 아래에 자동 묶음 동작과 새 버전 추가 방법 보충 |

- 이제 새 버전은 **마지막 묶음 끝에 칩만 추가**하면 16번째부터 새 탭이 자동으로 생깁니다. 마크업 묶음이 어긋나도 화면에는 15개씩 표시됩니다.
- 이전/다음 버튼으로 탭 경계를 넘으면 해당 탭이 따라 열리는 기존 동작은 그대로입니다.

## 검증 결과 (실제 Chrome 1280×800)

| 확인 | 현재 파일 | 묶음 마크업을 모두 없앤 사본 |
|---|---|---|
| 탭 이름 | v1.0 – v1.14 / v1.15 – v1.29 / v1.30 – v1.31 | 동일 |
| 탭별 개수 | 15 / 15 / 2 | 동일 |
| 처음 화면 | v1.31 · 32/32, 세 번째 탭 열림 | 동일 |
| 이전 3번 | v1.28 · 29/32, 두 번째 탭으로 이동 | 동일 |
| 첫 탭 클릭 | 첫 번째 탭 열림 | 동일 |
| 오류 | 0건 | 0건 |

(위 표는 v1.32 칩 추가 전 결과. 추가 후 다시 확인한 결과: 두 파일 모두 탭 `v1.0 – v1.14 / v1.15 – v1.29 / v1.30 – v1.32`, 개수 15 / 15 / 3, 처음 화면 v1.32 · 33/33, 이전 3번 → v1.29 · 두 번째 탭, 오류 0건)

## 커밋·푸시

후속 요청 `커밋하고 푸시해줘`에 따라 `mapservice-rest`를 커밋하고 `origin/main`에 푸시했습니다. `CLAUDE.md`에는 사용자가 직접 추가한 "한 탭에 15개" 규칙과 이번에 보충한 자동 묶음 설명이 함께 포함됩니다.
