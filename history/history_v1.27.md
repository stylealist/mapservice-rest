# history v1.27 — 로컬에서도 첨부(사진·오디오·영상)가 재생되도록 계정 주입 자동화

- **날짜**: 2026-09-17
- **영향 저장소**: `mapservice-rest`(로컬 기동 스크립트·문서)
- **이전 버전**: [history_v1.26.md](history_v1.26.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 로컬 첨부 중계 | http://localhost:8100/map/qfield/facilities/FACIL_T18_10/media?path=DCIM%2FJPEG_20260916071830596.jpg | 이번에 200으로 정상화 |
| 실서버 첨부 중계 | https://api.sj-lab.co.kr/map/qfield/facilities/FACIL_T18_10/media?path=DCIM%2FJPEG_20260916071830596.jpg | v1.25 Secret 생성 후 정상 |

## 실행한 프롬프트

```
이전에 반영했던 실서버에서 사진, 오디오, 비디오가 안나오는 이슈가
실서버에서는 문제가없는데 로컬에서 나오지않는 오류가 있어.
로컬에서도 실서버에서도 이미지, 오디오, 비디오를 확인 할 수 있도록 해줘
```

## 원인 — 계정 주입 경로가 운영에만 생겼음

v1.25에서 운영은 `qfield-credentials` Secret으로 계정이 들어가게 됐고, 실제로 Secret이 생성되어 정상화됐습니다.
반면 **로컬은 계정을 주입할 방법이 "띄우기 전에 셸에서 직접 환경변수를 넣는 것"뿐**이었습니다.
그래서 `local-stack.ps1 start`만 하면 계정 없이 뜨고, 미디어 엔드포인트만 503(`NOT_CONFIGURED`)이 됐습니다.

확인한 상태(변경 전):

| 대상 | 사진 | 오디오 | 영상 |
|---|---|---|---|
| 로컬 | 503 | 503 | 503 |
| 실서버 | 200 `image/jpeg` | 200 `audio/mp4` | 200 `video/mp4` |

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `scripts/local-stack.ps1` | `loadQfieldCredentials` 추가 — 백엔드 기동 직전에 계정을 환경변수로 채움 |
| `.claude/settings.local.json` | QField 계정 3개 추가 (**`.gitignore` 대상, 커밋되지 않음**) |
| `CLAUDE.md` | 계정 주입 경로(로컬·운영)와 "첨부만 안 나오면 계정부터 확인" 안내 추가 |
| `docs/dev-environment.md` | 첨부 재생용 환경변수 절을 자동 주입 기준으로 갱신, 운영 Secret 적용 완료 반영 |
| `docs/mcp.md` | `settings.local.json`이 MCP 전용이 아니라 이 저장소의 로컬 비밀값 보관처임을 명시 |

읽는 순서는 **① 이미 설정된 환경변수 → ② `.claude\settings.local.json`의 `env`** 입니다.
셸에서 직접 넣은 값이 있으면 그대로 존중하고, 없을 때만 파일에서 채웁니다.

```
  QField 계정 적용: stylealist (첨부 재생 가능)          ← 계정이 들어간 경우
  주의: QField 계정이 없어 첨부(사진·음성·영상) 재생은 503 입니다.   ← 없는 경우
```

**저장소 파일에는 값을 적지 않았습니다.** 이 저장소는 public이라 계정 값은 `.gitignore` 대상인
`.claude/settings.local.json`에만 두고, 스크립트는 그 파일을 읽기만 합니다.

## 검증 결과

### 엔드포인트 (로컬·실서버 동일 파일 비교)

| 대상 | 종류 | 상태 | Content-Type | 크기 |
|---|---|---|---|---|
| 로컬 | 사진 | 200 | `image/jpeg` | 84KB |
| 로컬 | 오디오 | 200 | `audio/mp4` | 154KB |
| 로컬 | 영상 | 200 | `video/mp4` | 1,121KB |
| 실서버 | 사진 | 200 | `image/jpeg` | 84KB |
| 실서버 | 오디오 | 200 | `audio/mp4` | 154KB |
| 실서버 | 영상 | 200 | `video/mp4` | 1,121KB |

크기가 바이트 단위까지 같아 같은 원본을 받아오는 것이 확인됩니다.

### 브라우저 (실제 Chrome, 로컬 상세 팝업)

| 항목 | 결과 |
|---|---|
| 사진 | 1080×1920 로드, 화면에 표시됨 |
| 오디오 | 길이 9.7초, 재생 시작됨(`paused=false`), 오류 없음 |
| 영상 | 길이 2.6초, 1080×1920, 재생 시작됨, 오류 없음 |
| 미디어 요청 | 3건 모두 200 (`image/jpeg`·`audio/mp4`·`video/mp4`) |
| 콘솔 오류 | 0건 |

## 참고

- 계정 값을 바꿔야 하면 `.claude/settings.local.json`의 `env`만 고치고 스택을 다시 띄우면 됩니다.
- 새로 참여하는 사람은 그 파일이 없으므로 첨부만 503이 됩니다 — 기동 로그의 `주의:` 줄이 그 안내입니다.
