# history v2.11 — 배포 때 지도(sj-lab-mapservice)가 지워지는 원인 규명과 재발 방지

- **날짜**: 2026-09-23
- **영향 저장소**: `mapservice-rest`(문서·스크립트). Jenkins 잡 수정은 사용자가 직접 적용
- **이전 버전**: [history_v2.10.md](history_v2.10.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 운영 허브 | https://sj-lab.co.kr/ | `html/` |
| 운영 지도 | https://sj-lab.co.kr/map/ | `html/map/` — 허브의 하위 폴더 |
| 점검 스크립트 | `scripts/check-prod-sites.ps1` | 이번에 신설 |
| 조치 문서 | `docs/deploy-static-sites.md` | 이번에 신설 |

## 실행한 프롬프트

```
해당 프로젝트에 추가할만한 기능이 있을까?
배포 때 sj-lab-mapservice가 지워지는 문제 해결해줘
배포 스크립트 첨부할게 (지도 배포 파이프라인 전문)
```

> **정정(v2.13)**: 아래에서는 허브 잡이 `rm -rf $DIR_FRONT/*` 패턴을 쓴다고 **추정**했지만, 실제 허브 파이프라인을 받아 확인한 결과 원인은 `rm -rf`가 아니라 **`sshTransfer`의 `cleanRemote: true`가 최종 웹 디렉터리(`.../html`)를 대상으로 걸려 있던 것**이었습니다. 지우는 주체가 허브 잡이라는 결론은 그대로입니다. 확정된 내용은 [history_v2.13.md](history_v2.13.md) 참고.

## 원인

정적 프론트 두 개가 **웹서버 노드의 같은 트리**에 파일로 복사되는 구조입니다.

```
/home/kuber-volume/sj-lab-webserver/html/       ← 허브(sj-lab-hub)
/home/kuber-volume/sj-lab-webserver/html/map/   ← 지도(sj-lab-mapservice)  ※ 허브의 하위 폴더
```

- 첨부받은 **지도 배포 잡**은 `rm -rf $DIR_FRONT/*`의 대상이 `html/map`이라 자기 폴더만 비웁니다 → 허브를 지우지 않습니다.
- 따라서 지도가 사라졌다면 **상위 디렉터리(`html`)를 비우는 잡**, 즉 **허브 배포 잡**이 같은 패턴을 써서 그 안의 `map/`까지 지운 것입니다.
- nginx가 `try_files $uri $uri/ /index.html`로 SPA 폴백을 하므로, 지도가 지워져도 `/map/`은 **404가 아니라 허브 첫 화면을 HTTP 200**으로 돌려줍니다 → "지워졌다"는 사실이 로그·모니터링에 드러나지 않았습니다.

## 확인한 사실 (운영 응답 기준, 2026-09-23)

| 확인 | 결과 |
|---|---|
| `/` · `/map/` | 각각 제 파일로 정상 서빙(현재는 지워지지 않은 상태) |
| `/map/` 파일들의 Last-Modified | 전부 동일 → 배포가 트리 전체를 한 번에 덮어쓰는 방식 |
| 없는 경로(`/does-not-exist-xyz`) | **200 + 허브 index.html** (폴백이 404를 감춤) |
| `/map/CLAUDE.md`, `/map/docs/*.md`, `README.md`, `AGENTS.md` | **200 — 저장소 문서가 웹에 공개됨** (`sourceFiles: '**/*'`로 통째 복사) |
| `/map/.git/config`, `/map/.claude/*` | 폴백(복사되지 않음) — 노출 없음 |
| 운영 응답 헤더 | `Server: nginx/1.24.0 (Ubuntu)` — 차트 이미지는 `nginx:latest`이므로 **공개 트래픽은 노드의 nginx가 처리**할 가능성이 큼(차트만 고치면 반영 안 될 수 있음) |

## 작업 내용

| 항목 | 파일 | 내용 |
|---|---|---|
| 조치 문서 신설 | `docs/deploy-static-sites.md` | 디렉터리 구조, 원인, **허브 잡 안전 패턴**(rsync `--exclude 'map/'` 또는 `find ... ! -name map`), **지도 잡 원자적 교체**(새 폴더 → `mv` 교체), 배포 후 자동 확인, 복구 방법, 근본 분리(별도 디렉터리 + nginx `alias`) 순서 |
| 점검 스크립트 신설 | `scripts/check-prod-sites.ps1` | 허브 표식(webpack `bundle`)·지도 표식(`js/auth-gate.js`, `openlayers/ol.js`)으로 판별. **지도 경로에서 허브 표식이 나오면 실패** 처리 |
| 구조 문서 갱신 | `docs/system-architecture.md` | 배포 경로 절에 "정적 프론트는 이미지·ArgoCD 경로가 아님 + 지워짐 현상" 추가 |
| 허브 규칙 갱신 | `CLAUDE.md` | 문서 목록에 `docs/deploy-static-sites.md` 추가 |

**금지 패턴으로 명시**: `rm -rf $DST/*` (하위 `map/`까지 삭제, 변수가 비면 `rm -rf /*` 위험).

## 검증

| 확인 | 결과 |
|---|---|
| `scripts\check-prod-sites.ps1` 실행 | 허브 200·지도 200, "두 사이트 모두 제 파일로 서빙 중" (exit 0) |
| 폴백 판정 로직 | 지도 경로에서 허브 표식 검출 시 실패하도록 구성(현재는 미검출) |
| 운영 URL 프로브 | 위 "확인한 사실" 표대로 실제 응답으로 확인 |

## 남은 작업 (사용자 적용 필요)

- **허브 배포 잡의 `execCommand` 교체** — 이번 수정의 핵심. 문서의 rsync/`find` 패턴 중 하나로 바꿔야 재발이 멈춥니다. 허브 파이프라인 원문을 받으면 그대로 고쳐 드릴 수 있습니다.
- 지도 배포 잡을 원자적 교체 + 문서 제외(`--exclude`)로 교체.
- 배포 잡 마지막에 `curl -fsS .../map/ | grep -q 'js/auth-gate.js'` 확인 단계 추가.
- (선택) 지도를 `html/map`에서 `sj-lab-webserver/map`으로 분리 + nginx `alias` — 적용 전에 실제 서빙 중인 nginx를 `nginx -T`로 확인할 것.
