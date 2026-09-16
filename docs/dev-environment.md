# sj-lab 통합 개발 환경

`mapservice-rest`는 sj-lab 여러 저장소를 넘나드는 작업(프론트엔드 + 백엔드 + 게이트웨이 + 디스커버리)을 총괄하는 기준 저장소입니다. 총괄 Claude 세션은 이 저장소에서 띄우고, 저장소를 넘나드는 설정·문서(MCP, DB 분석, 로컬 개발 구성, 시스템 전체 구조)는 여기에 둡니다. 각 저장소의 코드 규칙은 그 저장소의 `CLAUDE.md`를 따릅니다. 계층별 역할과 API 계약은 `docs/system-architecture.md`를 봅니다.

## 로컬 저장소 경로

| 저장소 | 로컬 경로 | 역할 |
|---|---|---|
| `mapservice-rest` | `C:\developer\workspace\mapservice-rest` | 지도/GIS GeoJSON API (이 저장소, 총괄 기준) |
| `sj-lab-apigateway` | `C:\developer\workspace\sj-lab-apigateway` | Spring Cloud Gateway (라우팅·CORS) |
| `sj-lab-discoveryServer` | `C:\developer\workspace\sj-lab-discoveryServer` | Eureka 서비스 레지스트리(네이밍 서버) |
| `sj-lab-scheduler` | `C:\developer\workspace\sj-lab-scheduler` | 공공 API 수집 배치 → DB 적재 + 지도용 `map.v_*_geojson` 뷰 생성 |
| `fast-api-ai` | `C:\developer\workspace\fast-api-ai` | FastAPI 서비스(AI/RAG 예정, 현재 골격) |
| `sj-lab-mapservice` | `C:\vscode_develop\sj-lab-mapservice` | 지도 프론트엔드(정적 SPA, OpenLayers) |
| `sj-lab-hub` | `C:\vscode_develop\sj-lab-hub` | sj-lab 사이트 첫 화면(React + Webpack 랜딩 허브, `npm start` 포트 3000) |
| `sj-lab-k8s-manifests` | `C:\developer\workspace\sj-lab-k8s-manifests` | 쿠버네티스 배포 매니페스트(서비스별 Helm 차트). ArgoCD의 GitOps 소스 |
| `infra-manage-app` | `C:\vscode_develop\infra-manage-app` | 현장조사 앱(QField 포크, C++/QML). 기본 브랜치 `master` |
| `sj-qfieldsync` | `C:\vscode_develop\sj-qfieldsync` | QFieldCloud → PostGIS `qfield` 스키마 동기화 배치(파이썬, 30초 주기) |

이 저장소를 제외한 아홉 저장소는 `.claude/settings.local.json`의 `permissions.additionalDirectories`에 등록되어 총괄 세션에서 바로 읽고 수정할 수 있습니다. 경로가 바뀌면 그 목록도 함께 고칩니다.

**폴더 신뢰(2026-09-15 등록, 2026-09-16 2곳 추가)**: 위 저장소 전부를 도구별 신뢰 목록에 미리 등록해 두었습니다. 새 저장소를 추가하면 세 곳 모두에 넣어야 워커가 신뢰 확인 창에서 멈추지 않습니다.
- Claude Code: `~/.claude.json`의 `projects["C:/..."].hasTrustDialogAccepted: true` (Orca 등에서 `c:/...` 소문자 드라이브 키로도 따로 생기므로 둘 다 확인)
- Antigravity CLI(agy): `~/.gemini/antigravity-cli/settings.json`의 `trustedWorkspaces` (`C:\\...` 형식)
- Gemini CLI: `~/.gemini/trustedFolders.json` (`"c:/...": "TRUST_FOLDER"`)

**Orca 프로젝트 목록**: 위 열 저장소는 Orca에 `orca repo add --path <경로>`로 등록되어 사이드바에 표시됩니다(`orca repo list --json`으로 확인). 새 저장소도 같은 방식으로 추가합니다.

그 밖의 저장소(nginx 등)는 GitHub `stylealist/*`에 있으며, 로컬 경로는 확인되는 대로 이 표와 `additionalDirectories`에 추가합니다.

## 로컬 실행 구성 (포트·라우팅)

| 포트 | 프로세스 | 비고 |
|---|---|---|
| `8100` | API Gateway (`ApigatewayServiceApplication`, IntelliJ 실행) | `/map/**` → `lb://MAPSERVICE-REST` |
| `8761` | Eureka Discovery (`DiscoveryserviceApplication`) | `local` 프로파일은 자기 자신을 등록하지 않음. 대시보드 `http://localhost:8761` |
| 랜덤(`server.port: 0`) | `mapservice-rest` (IntelliJ 실행, 여러 인스턴스 가능) | Eureka 등록 후 게이트웨이로만 접근 |
| 랜덤(`server.port: 0`) | `sj-lab-scheduler` (context-path `/scheduler`) | 게이트웨이 `/scheduler/**`. 기동만 해도 cron 배치가 실제 DB에 적재하므로 검증용으로 함부로 띄우지 말 것 |
| `8000` | `fast-api-ai` (`python main.py`, `root_path=/fast-api-ai`) | 게이트웨이 `/fast-api-ai/**`. 로컬은 Eureka에 `127.0.0.1`로 등록 |
| `4000` | 프론트엔드 정적 서버(node, 또는 `python -m http.server 4000`) | 게이트웨이 CORS 허용 origin |

**기동 순서**: Eureka(8761) → mapservice-rest → 게이트웨이(8100) → 프론트(4000). 게이트웨이·백엔드는 반드시 `local` 프로파일로 띄워야 Eureka 주소(`localhost:8761`)가 잡힙니다(게이트웨이는 프로파일이 없으면 Eureka 주소가 비어 있음). 백엔드가 막 뜬 직후에는 게이트웨이의 레지스트리 캐시가 갱신될 때까지 잠시 503이 날 수 있으니, Eureka 대시보드에서 `MAPSERVICE-REST`가 UP인지 먼저 확인합니다.

**IntelliJ 없이 한 번에 기동**: `scripts\local-stack.ps1`이 위 순서대로 띄웁니다(JDK 17 자동 탐색, `local` 프로파일 고정).

```
powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 start     # 빌드 후 전체 기동
powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 start -NoBuild
powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 status
powershell -ExecutionPolicy Bypass -File scripts\local-stack.ps1 stop      # 이 스크립트가 띄운 프로세스만 종료
```

- 이미 포트가 사용 중이면(예: IntelliJ로 실행 중) 그 구성요소는 건너뜁니다. `stop`은 `.local-stack\pids.json`에 기록된 프로세스만 종료하므로 IntelliJ 프로세스는 건드리지 않습니다.
- 로그·pid는 `.local-stack\`(git 제외)에 쌓입니다. `sj-lab-discoveryServer`는 `target/`이 git에 추적되므로 `.local-stack\build\` 복사본에서 빌드합니다.
- scheduler는 DB 적재 때문에 이 스크립트에 넣지 않았습니다.
- **백엔드만 재기동하면 Eureka에 죽은 인스턴스가 남아** 게이트웨이 요청의 절반이 500이 됩니다(리스 만료까지 1~3분). 기다리거나, 죽은 인스턴스를 직접 해제하세요: `Invoke-WebRequest -Method Delete "http://localhost:8761/eureka/apps/MAPSERVICE-REST/<instanceId>"` (인스턴스 목록은 `http://localhost:8761/eureka/apps/MAPSERVICE-REST`, Accept: application/json).

**운영 대응**: 프론트 `https://sj-lab.co.kr` → `https://api.sj-lab.co.kr/map/...`(게이트웨이) → Eureka `https://eureka.sj-lab.co.kr`. 운영 프로파일은 저장소 밖(Helm/ConfigMap)에서 주입됩니다.

- 프론트엔드의 `getApiUrl()`은 로컬에서 `http://localhost:8100` + `/map/...`을 호출하므로, **8100은 백엔드가 아니라 게이트웨이**입니다.
- 게이트웨이 CORS 허용 origin은 `http://localhost:4000`, `https://sj-lab.co.kr`, `https://www.sj-lab.co.kr`뿐입니다. 프론트엔드를 다른 포트(예: `python -m http.server 8000`)로 띄우면 API 호출이 403으로 막힙니다.
- 새 API 동작 검증은 게이트웨이를 통해 `Origin: http://localhost:4000` 헤더로 호출해 확인합니다.

## 반드시 지킬 것

- 백엔드 작업은 반드시 `C:\developer\workspace\mapservice-rest`에서 할 것 — 다른 경로에 clone한 복사본에서 작업하지 말 것.
- 에이전트가 검증용 서버를 따로 띄울 때 `8100`(게이트웨이)·`8761`(Eureka)을 쓰거나 IntelliJ로 띄운 프로세스를 종료하지 말 것. 별도 포트(예: `--server.port=8101`)로 띄우고 검증 후 그 프로세스만 종료할 것.
- Orca로 Antigravity 워커를 신뢰 목록에 없는 폴더에 띄우면 "Do you trust the contents of this project?" 확인에서 멈춥니다. 위 여섯 저장소는 미리 등록되어 있으니, 다른 폴더(새 worktree 포함)에서 멈추면 위 신뢰 목록에 추가하거나 사용자가 해당 탭에서 승인하고, 실패한 Dispatch는 `worker-start --retry-of`로 교체합니다.
- 저장소를 넘나드는 설정·문서가 바뀌면 이 문서와 `docs/mcp.md`를 코드 변경 직후 갱신할 것.
