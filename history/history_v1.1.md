# history v1.1 — sj-lab 멀티 저장소 총괄 환경 구성

- **날짜**: 2026-09-15 ~ 2026-09-16
- **기준 저장소**: `C:\developer\workspace\mapservice-rest` (총괄 허브)
- **영향 저장소**: mapservice-rest, sj-lab-apigateway, sj-lab-discoveryServer, sj-lab-scheduler, fast-api-ai, sj-lab-mapservice

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 브라우저로 바로 열기 |
| 공유 웹사이트(정적 서버) | http://localhost:4100 | `python -m http.server 4100 --directory history/web` |
| 지도 프론트엔드 | http://localhost:4000 | 로컬 스택 기동 후 |
| API Gateway | http://localhost:8100/map/admin-area/sido | 게이트웨이 경유 API 확인 |
| Eureka 대시보드 | http://localhost:8761 | 서비스 등록 상태 |

## 실행한 프롬프트

1. `/orchestration 현재 mapservice-rest 서버인데 관련있는 서버들을 설정하려고 하거든 msa 개발이라 서버에서는 apigateway서버인 C:\developer\workspace\sj-lab-apigateway, C:\developer\workspace\sj-lab-discoveryServer 네이밍 서버, 그리고 프론트인 C:\vscode_develop\sj-lab-mapservice 인데 .md 파일에 해당부분을 기입해서 db ~ 프론트까지 시스템 전체를 바라보고 바이브코딩을 하고싶어`
2. `내가 말한 경로들에 대한 정보는 어떤 파일에 담은거야?`
3. `C:\developer\workspace\sj-lab-scheduler 스케쥴러와 C:\developer\workspace\fast-api-ai fast api로 추가했어 해당 경로도 추가후 이전에 작업했던 폴더를 포함해서 모든 폴더에 신뢰 확인도 진행해줘`
4. `각 폴더들 프로젝트도 실행해서 왼쪽 프로젝트 목록에 나오도록 해줄수있어?`
5. `해당 프로젝트를 실행해서 chrome에서 기능이 정상적으로 되는지 확인해볼수있나`
6. `어디로 접속하면 확인가능해?`
7. `apigateway, eureka 서버도 intellij에서 실행하지않고 수행되도록 해줘`

## 변경된 결과물

### 새로 만든 파일

| 파일 | 내용 |
|---|---|
| `docs/system-architecture.md` | DB → 백엔드 → Eureka → 게이트웨이 → 프론트 전체 구조도, 계층별 저장소 표, API 계약 표(11개 엔드포인트), 저장소를 넘나드는 변경 체크리스트, 다른 저장소를 다룰 때의 규칙 |
| `scripts/local-stack.ps1` | IntelliJ 없이 로컬 스택을 기동/중지/상태확인하는 스크립트 (`start` / `stop` / `status`) |

### 수정한 파일

| 파일 | 변경 내용 |
|---|---|
| `CLAUDE.md` | `@docs/system-architecture.md` 로드 추가, 다른 저장소 수정 전 그 저장소 CLAUDE.md를 먼저 읽는 규칙, API 변경 시 백엔드·프론트 동시 수정 규칙 |
| `docs/dev-environment.md` | 저장소 경로 표에 discoveryServer·scheduler·fast-api-ai 추가, 포트 표 보강, 기동 순서와 운영 도메인 흐름, 폴더 신뢰 등록 위치, Orca 프로젝트 등록 방법 |
| `.gitignore` | `.local-stack/`(스크립트 실행 로그·pid·빌드 복사본) 제외 |
| `.claude/settings.local.json` | `permissions.additionalDirectories`에 5개 저장소 등록 (로컬 전용, 커밋 제외) |
| `sj-lab-apigateway/CLAUDE.md` | "통합 허브" 섹션 추가 (허브 저장소와 문서 위치 안내) |
| `sj-lab-discoveryServer/CLAUDE.md` | "통합 허브" 섹션 추가 |
| `sj-lab-scheduler/CLAUDE.md` | "통합 허브" 섹션 추가 |
| `fast-api-ai/CLAUDE.md` | "통합 허브" 섹션 추가 |

### 설정 변경 (저장소 밖)

- **폴더 신뢰**: 여섯 저장소를 Claude Code(`~/.claude.json`), Antigravity CLI(`~/.gemini/antigravity-cli/settings.json`), Gemini CLI(`~/.gemini/trustedFolders.json`) 세 곳에 등록. 워커가 "Do you trust the contents of this project?"에서 멈추지 않음.
- **Orca 프로젝트 목록**: `orca repo add`로 5개 저장소 등록 → 사이드바에 6개 표시.

## 검증 결과

- **로컬 스택 기동**: `scripts\local-stack.ps1 start`로 Eureka(8761) → mapservice-rest(랜덤 포트) → 게이트웨이(8100) → 프론트(4000) 순차 기동 성공. Eureka 등록 확인: `APIGATEWAY-SERVICE(1)`, `MAPSERVICE-REST(1)`.
- **백엔드 API 직접 호출**: WFS 6종 200, 행정구역 3종 200, 시설물 200(2,474건), 없는 시설물 404, 잘못된 코드 400 — 모두 의도대로 동작.
- **Chrome 프론트 확인**: 페이지 로드와 지도·WFS·시설물 모듈 초기화 정상.

## 발견한 문제 (미수정)

1. **WFS 응답 과대**: 버스정류장 86MB/59초(20만 건), 병원 59MB/40초(6.4만 건), 약국 19MB/12초, 편의점 17MB/13초. 전체 데이터를 한 번에 내려주는 구조.
2. **프론트 디버그 진입점 버그**: `window.mapInstance`가 항상 `undefined` (`map-core.js:154`, `map.js:104`에서 지도 생성 전에 할당). `window.getMap()`은 정상. 프론트 CLAUDE.md에는 `window.mapInstance`로 안내되어 있음.

## 다음 작업 후보

- Chrome에서 게이트웨이 경유 전체 기능 검증(시설물 필터, WFS 레이어 6종 토글) — 사용자 중단으로 미실행.
- WFS 대용량 응답 개선 검토(BBOX/줌 기반 필터링, 압축).
- `window.mapInstance` 버그 수정 여부 결정.
