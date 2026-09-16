# history v1.8 — 작업 저장소 2곳 추가(k8s-manifests, hub)와 배포 경로 문서화

- **날짜**: 2026-09-16
- **영향 저장소**: `mapservice-rest`(문서), `sj-lab-k8s-manifests`, `sj-lab-hub`
- **이전 버전**: [history_v1.7.md](history_v1.7.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 실서버 지도 | https://sj-lab.co.kr | v1.7까지의 변경이 배포 완료 |
| 실서버 API | https://api.sj-lab.co.kr/map/qfield/facility-icons | 200 · 7건 |

## 실행한 프롬프트

```
저장소환경을 2곳 추가하고싶어 C:\developer\workspace\sj-lab-k8s-manifests 쿠버네티스, C:\vscode_develop\sj-lab-hub sj-lab 사이트의 첫 화면이야
```

## 추가한 저장소

| 저장소 | 경로 | 역할 |
|---|---|---|
| `sj-lab-k8s-manifests` | `C:\developer\workspace\sj-lab-k8s-manifests` | 서비스별 Helm 차트 13개. ArgoCD가 이 저장소를 GitOps 소스로 동기화(selfHeal·prune) |
| `sj-lab-hub` | `C:\vscode_develop\sj-lab-hub` | sj-lab 사이트 첫 화면. React 18 + Webpack 랜딩 허브(`npm start` 3000) |

## 등록 내용

기존 저장소와 같은 절차를 그대로 적용했습니다.

| 항목 | 위치 | 결과 |
|---|---|---|
| 작업 범위 | `.claude/settings.local.json`의 `additionalDirectories` | 7곳 |
| Claude Code 신뢰 | `~/.claude.json` | 등록 |
| Antigravity CLI 신뢰 | `~/.gemini/antigravity-cli/settings.json` | 9곳 |
| Gemini CLI 신뢰 | `~/.gemini/trustedFolders.json` | 8곳 |
| Orca 프로젝트 목록 | `orca repo add` | 8곳 |

## 변경된 문서

| 파일 | 내용 |
|---|---|
| `docs/dev-environment.md` | 저장소 경로 표에 2곳 추가, 신뢰·Orca 등록 개수 갱신 |
| `docs/system-architecture.md` | 계층 표에 "첫 화면"·"배포" 행 추가, **배포 경로 절 신설**, 저장소별 주의사항 2건 추가 |
| `CLAUDE.md` | 다른 저장소 수정 전 해당 CLAUDE.md를 먼저 읽는 규칙에 2곳 포함 |
| `sj-lab-k8s-manifests/CLAUDE.md`, `sj-lab-hub/CLAUDE.md` | "통합 허브" 안내 섹션 추가 |

## 새로 문서화한 배포 경로

```
git push → Jenkins(빌드 → 이미지 push: NCP 레지스트리)
        → sj-lab-k8s-manifests 의 <서비스>/values.yaml 의 image.tag 자동 커밋
        → ArgoCD 동기화 → 쿠버네티스 롤아웃
```

이번 실서버 반영 과정에서 겪은 두 가지를 함께 적었습니다.

1. **동시 push 시 매니페스트 커밋 충돌** — 매니페스트 단계가 clone → sed → push 구조라, 여러 저장소를 한꺼번에 push하면 한 잡이 `cannot lock ref`로 실패합니다. 실제로 mapservice-rest 잡이 실패해 이미지 46은 레지스트리에 있는데 태그 커밋만 빠졌습니다. 잡을 재실행하면 복구됩니다.
2. **롤아웃 중 503** — 옛 파드 종료와 새 파드의 Eureka 등록 사이에 게이트웨이가 잠시 503을 반환합니다. 배포 직후 503은 몇 초 뒤 다시 확인하면 됩니다.

저장소별 주의사항도 적었습니다 — k8s-manifests는 수정 전 `git pull`(Jenkins 자동 커밋이 계속 쌓임), 수정 후 `helm lint`·`helm template`, `image.tag` 임의 변경 금지. hub는 `src/App.js`의 `features` 배열이 단일 소스이고 검증은 `npm start`(3000).

## 확인한 상태

- 두 저장소 모두 작업 트리 깨끗함. **k8s-manifests 로컬은 원격보다 7커밋 뒤처져 있음**(Jenkins 태그 커밋) — 수정 전 `git pull` 필요.
- v1.7까지의 변경은 실서버 반영 완료: `/map/qfield/facility-icons` 200·7건, `/map/qfield/facilities` 200·2,474건, CORS 헤더 정상.

## 참고

- 실서버 시설물 목록 응답이 **20.6초**(894KB)로 로컬(1.5초)보다 느립니다. 원인 확인이 필요한 항목으로 남겨 둡니다.
- 커밋·push는 하지 않았습니다.
