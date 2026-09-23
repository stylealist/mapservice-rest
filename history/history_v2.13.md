# history v2.13 — 지도 삭제 원인 확정(`cleanRemote: true`)과 실제 잡 기준 교체본

- **날짜**: 2026-09-23
- **영향 저장소**: `mapservice-rest`(문서). Jenkins 잡 반영은 사용자가 직접
- **이전 버전**: [history_v2.12.md](history_v2.12.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 운영 허브 | https://sj-lab.co.kr/ | 배포 대상 `html/` |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 대상 `html/map/` |
| 허브 잡 교체본(최종) | `docs/jenkins/sj-lab-hub-pipeline.groovy` | 실제 잡 기준으로 재작성 |
| 지도 잡 교체본 | `docs/jenkins/sj-lab-mapservice-pipeline.groovy` | v2.12와 동일 |

## 실행한 프롬프트

```
(기존 지도 jenkins 파이프라인 전문)
이게 기존 map쪽 jenkins고
(기존 hub jenkins 파이프라인 전문)
이게 기존 hub jenkins인데 고려해서 문제없이 수정한게맞는거야?
```

## 확정된 원인 (v2.11 추정에서 정정)

허브 잡에는 `rm -rf`가 **없었습니다**. 원인은 전송 옵션이었습니다.

```groovy
remoteDirectory: "${env.REMOTE_DIR}",   // = /home/kuber-volume/sj-lab-webserver/html
cleanRemote: true                        // 전송 전에 이 디렉터리의 파일·하위 디렉터리를 모두 삭제
```

- Publish over SSH의 `cleanRemote`는 **대상 디렉터리를 통째로 비운 뒤** 전송합니다. 허브 잡은 빌드 산출물을 **최종 웹 디렉터리로 직접** 전송하므로, 그 하위의 `html/map`(지도 전체)이 **매 허브 배포마다** 삭제됐습니다.
- 지도 잡에도 `cleanRemote: true`가 있으나 대상이 스테이징(`/respal/deploy`)이라 무해합니다. 지도 잡의 `rm -rf $DIR_FRONT/*`도 대상이 `html/map`이라 허브를 지우지 않습니다.
- 결론적으로 **"지우는 주체는 허브 잡"이라는 v2.11의 결론은 유지**되고, 메커니즘만 `rm -rf` → `cleanRemote`로 정정됩니다.

## 작업 내용

| 파일 | 내용 |
|---|---|
| `docs/jenkins/sj-lab-hub-pipeline.groovy` | **실제 잡 기준으로 재작성** — 원본의 `tools { nodejs 'node-18' }`, `npm install`, `post` 블록을 그대로 두고 배포 단계만 교체. 전송 대상을 스테이징(`/respal/deploy-hub`)으로 돌리고(`cleanRemote`는 여기서만), `execCommand`에서 `rsync -a --delete --exclude 'map/'`(없으면 `find … ! -name map`)로 최종 반영. `test -f build/index.html` 가드와 `Verify` 단계 추가. 파일 끝에 **1줄 대안**(`cleanRemote: false`)도 명시 |
| `docs/deploy-static-sites.md` | 원인 절을 `cleanRemote` 기준으로 정정, 조치 1에 "가장 빠른 방법(1줄)"과 "권장 방법(스테이징 경유)" 구분, 금지 패턴에 `cleanRemote: true` + 최종 웹 디렉터리 조합 추가 |
| `history/history_v2.11.md`, `history_v2.12.md` | 맨 앞에 정정 안내 추가(추정 → 확정 경위) |

## 두 가지 선택지

| 방법 | 변경량 | 효과 | 한계 |
|---|---|---|---|
| `cleanRemote: true` → `false` | 1줄 | 지도 삭제 즉시 중단 | 최종 위치를 정리하지 않아 산출물 이름이 바뀌면 옛 파일이 남음(현재 `index.html`+`bundle.js` 고정이라 영향 작음) |
| 스테이징 경유 + `rsync --exclude 'map/'` | 배포 단계 교체 | 지도 보호 + 옛 파일 정리 + 배포 후 자동 확인 | 스테이징 경로를 지도 잡과 분리해야 함, SSH 전역 "Remote Directory"가 `/`라는 전제 확인 필요 |

## 검증

| 확인 | 결과 |
|---|---|
| 원인 확정 | 사용자 제공 허브 파이프라인에서 `cleanRemote: true` + `remoteDirectory=.../html` 조합 확인 |
| 지도 잡 무해성 | 대상이 `/respal/deploy`(스테이징)·`html/map`이라 허브에 영향 없음 확인 |
| 운영 현재 상태(`scripts\check-prod-sites.ps1`) | 허브 200 · 지도 200, 둘 다 제 파일 |
| 교체본 전제 | 허브 잡의 `tools nodejs 'node-18'`·`npm install`·`post` 블록 원본 유지 확인 |

## 남은 작업

- 허브 잡에 교체본(또는 최소 `cleanRemote: false`) 반영 → 허브 1회 배포 → `Verify` 통과 및 `/map/` 정상 확인.
- 지도 잡 교체본 반영 후 `/map/` 아래 저장소 문서가 404가 되는지 확인.
- (선택) 지도를 `html/map`에서 별도 디렉터리로 분리 + nginx `alias` — 실제 서빙 중인 nginx를 `nginx -T`로 확인 후.
