# history v1.16 — 작업 저장소 2곳 추가(QField 앱·동기화)와 시설물 데이터 출처 확정

- **날짜**: 2026-09-16
- **영향 저장소**: `mapservice-rest`(문서), `sj-qfieldsync`, `infra-manage-app`
- **이전 버전**: [history_v1.15.md](history_v1.15.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 시설물 데이터 확인 |

## 실행한 프롬프트

```
작업저장소를 추가하려는데 C:\vscode_develop\sj-qfieldsync는 qfield앱에서 추가된 데이터가 sync되는 기능이고 C:\vscode_develop\infra-manage-app는 qfield app이야 추가해줘
```

## 추가한 저장소

| 저장소 | 경로 | 역할 |
|---|---|---|
| `infra-manage-app` | `C:\vscode_develop\infra-manage-app` | 현장조사 앱. QField(QGIS 기반) 포크를 시설물 점검용으로 커스터마이징. C++/QML, CMake+vcpkg, 기본 브랜치 `master` |
| `sj-qfieldsync` | `C:\vscode_develop\sj-qfieldsync` | QFieldCloud의 조사 데이터(GPKG)를 30초 주기로 감지해 PostGIS `qfield` 스키마로 적재하는 파이썬 단일 워커 |

## 등록 내용

| 항목 | 결과 |
|---|---|
| 작업 범위(`additionalDirectories`) | 9곳 |
| Claude Code · Antigravity · Gemini 폴더 신뢰 | 등록(11곳 / 10곳) |
| Orca 프로젝트 목록 | 10곳 |
| 두 저장소의 `CLAUDE.md` | "통합 허브" 안내 추가 |

## 확정된 시설물 데이터 출처

그동안 `docs/system-architecture.md`에 **"`qfield.facility_total_view`의 출처 미확인"** 으로 남아 있던 부분이 해소됐습니다.

```
[현장조사 앱] infra-manage-app (QField 포크)
   → QFieldCloud
   → [동기화] sj-qfieldsync (30초 주기, 변경된 프로젝트만 GPKG 내려받아 적재)
   → PostGIS qfield 스키마  ※ 테이블 추가·삭제 시 facility_total_view 재생성
   → mapservice-rest → 게이트웨이 → 지도 프론트엔드
```

**`facility_total_view`의 정의 주체는 `sj-qfieldsync`** 입니다. 시설물 컬럼이 바뀌면 그 저장소부터 확인해야 합니다.

## 변경된 문서

| 파일 | 내용 |
|---|---|
| `docs/dev-environment.md` | 저장소 경로 표에 2곳 추가, 신뢰·Orca 등록 개수 갱신 |
| `docs/system-architecture.md` | 구조도에 앱·동기화 계층 추가, 계층 표 2행 추가, 데이터 출처 문단 신설, 저장소별 주의사항 2건 추가 |
| `CLAUDE.md` | 다른 저장소 수정 전 해당 CLAUDE.md를 먼저 읽는 규칙에 2곳 포함 |
| `sj-qfieldsync/CLAUDE.md`, `infra-manage-app/CLAUDE.md` | "통합 허브" 안내 섹션 추가 |

### 새로 적은 저장소별 주의사항

- **`sj-qfieldsync`** — QFieldCloud 메타 DB와 적재 대상 PostGIS **두 DB를 함께 다룹니다.** 문법 확인은 `python -m py_compile qfield_data_sync.py`. **로컬에서 워커를 돌리면 실제 DB에 적재되므로 사용자 확인 후에** 실행합니다.
- **`infra-manage-app`** — 업스트림 QField 포크라 **커스텀 변경은 최소 지점에 집중**하고 업스트림 구조를 유지합니다. 기본 브랜치가 `master`(다른 저장소는 `main`)이고, CMake+vcpkg 전체 빌드는 수 시간이 걸리므로 빌드 전에 기존 빌드 디렉터리와 대상 플랫폼을 확인합니다.

## 확인한 상태

두 저장소 모두 작업 트리 깨끗함. `infra-manage-app`은 기본 브랜치가 `master`라 git 작업 시 주의가 필요합니다.
