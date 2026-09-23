# history v2.12 — 허브·지도 Jenkins 배포 파이프라인 교체본 작성

- **날짜**: 2026-09-23
- **영향 저장소**: `mapservice-rest`(문서). Jenkins 잡 반영은 사용자가 직접
- **이전 버전**: [history_v2.11.md](history_v2.11.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 운영 허브 | https://sj-lab.co.kr/ | 배포 대상 `html/` |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 대상 `html/map/` |
| 허브 잡 교체본 | `docs/jenkins/sj-lab-hub-pipeline.groovy` | 이번에 신설 |
| 지도 잡 교체본 | `docs/jenkins/sj-lab-mapservice-pipeline.groovy` | 이번에 신설 |

## 실행한 프롬프트

```
jenkins 내용은 안바꿔도되는거야? 바꿔야한다면 바꾼내용 보내줘
```

## 답

**바꿔야 합니다.** v2.11에서 규명한 대로 지도를 지우는 주체는 **허브 배포 잡**이므로, 거기를 고치지 않으면 허브를 배포할 때마다 지도가 다시 사라집니다. 저장소 코드로는 막을 수 없는 지점입니다(배포 스크립트가 Jenkins 잡 안에만 있음).

> **정정(v2.13)**: 이 버전의 허브 교체본은 실제 잡을 받기 전에 작성한 것이라 전제가 달랐습니다(`npm ci`, `tools` 미사용, `rm -rf` 제거 전제). 실제 잡 기준으로 다시 맞춘 최종본은 [history_v2.13.md](history_v2.13.md)와 `docs/jenkins/sj-lab-hub-pipeline.groovy`에 있습니다.

## 작업 내용

| 파일 | 내용 |
|---|---|
| `docs/jenkins/sj-lab-hub-pipeline.groovy` | 허브 잡 전문. ① `rm -rf $DST/*` → `rsync -a --delete --exclude 'map/'`(없으면 `find ... ! -name map`) ② `npm ci && npm run build` 단계 — `build/`가 `.gitignore`라 Jenkins에서 빌드해야 함 ③ 스테이징 경로를 `/respal/deploy-hub`로 분리(지도 잡의 `/respal/deploy`와 겹치면 동시 실행 시 충돌) ④ `Verify` 단계 |
| `docs/jenkins/sj-lab-mapservice-pipeline.groovy` | 지도 잡 전문. ① 원자적 교체(새 폴더에 복사 → `mv`로 바꿔치기) — 기존 `rm -rf` 후 `cp` 방식의 빈 화면 구간·반쪽 배포 제거 ② `excludes`로 `docs/**`, `**/*.md`, `.claude/**` 제외 — 저장소 문서가 웹에 공개되던 문제 해결 ③ `Verify` 단계 |
| `docs/deploy-static-sites.md` | 두 교체본을 가리키는 표 추가 |

두 잡 모두 마지막 `Verify`에서 허브·지도 양쪽을 본문 표식으로 확인합니다(`bundle` / `js/auth-gate.js`). 한쪽 잡이 다른 쪽을 지우면 **그 잡이 빨간색으로 실패**해 즉시 드러납니다.

## 전제(적용 전 확인)

- SSH 서버(`sj-lab-master`)의 Jenkins 전역 "Remote Directory"가 `/`라고 가정했습니다. 다르면 `REMOTE_DIR`와 `execCommand`의 `SRC`를 함께 맞춰야 합니다.
- 허브 잡 에이전트에 `node`/`npm`이 있어야 합니다(없으면 NodeJS 플러그인 또는 docker agent).
- 노드에 `rsync`가 없으면 스크립트가 자동으로 `find` 방식으로 넘어갑니다(양쪽 모두 넣어 둠).

## 검증

| 확인 | 결과 |
|---|---|
| 운영 사이트 현재 상태(`scripts\check-prod-sites.ps1`) | 허브 200 · 지도 200, 둘 다 제 파일 |
| 교체본의 판별 표식 | 허브 `bundle`, 지도 `js/auth-gate.js` — 실제 운영 응답에서 존재 확인 |
| 지도 잡 전제 | 대상이 `html/map`이라 허브를 지우지 않음(첨부받은 원본 기준) |

## 남은 작업

- **허브 잡에 교체본 반영**(핵심) 후 허브를 한 번 배포해 `Verify` 통과 확인.
- 지도 잡 교체본 반영 후 `/map/CLAUDE.md`가 404가 되는지 확인(문서 노출 정리 확인).
- (선택) 지도를 `html/map`에서 별도 디렉터리로 분리 + nginx `alias` — 실제 서빙 중인 nginx를 `nginx -T`로 확인 후.
