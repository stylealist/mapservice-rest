# history v1.2 — 작업 규칙 추가 및 history/공유 웹사이트 체계 도입

- **날짜**: 2026-09-16
- **기준 저장소**: `C:\developer\workspace\mapservice-rest` (총괄 허브)
- **이전 버전**: [history_v1.1.md](history_v1.1.md)

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

```
- 한글로 설명해줘
- DB는 맘대로 DDL,DCL,DML등을 사용해서 수정하지말것
- 파일을 삭제할경우에는 따로 폴더를 하나 만들어서 삭제한 파일들을 담을것
- 자동으로 PUSH하지말것
- 변경한 사항을 돌렸을때마다 history폴더에 history_v1.1 history_v1.2~~ 이런식으로 버전관리를해서 넣어주고 내용은 내가 실행한 프롬프트내용과 변경된 결과물을 작성해줘
- 변경한 사항을 돌렸을때마다 history/web폴더에 history_v1.1를 참고해서 팀간에 공유가 가능하도록 웹사이트로 만들어줘
- C:\developer\workspace\disaster-rest-harness 경로에서안에서만 진행해줘
- history.md 파일들에 접속 url 링크도 추가해줘
 해당 부분을 추가하려고해
```

확인 질문에 대한 사용자 답변:
- 작업 경로 규칙: **"내가 파일에서 명시한 저장소에서만 진행"** (`disaster-rest-harness`는 다른 프로젝트의 규칙이라 제외)
- history 위치: **허브 저장소 안** (`mapservice-rest/history/`)
- 기록 시점: **파일을 바꿨을 때만** (조회·질문만 한 턴은 기록하지 않음)

## 변경된 결과물

### 수정한 파일

| 파일 | 변경 내용 |
|---|---|
| `CLAUDE.md` | "작업 규칙(2026-09-16 추가)" 항목 신설 — 작업 범위(명시된 저장소만), DB 읽기 전용(DDL/DCL/DML 금지), 파일 삭제 대신 `trash/<날짜>/`로 이동, 자동 push 금지, 파일 변경 시 history 버전 문서 작성, 공유 웹사이트 동시 갱신 |

### 새로 만든 파일

| 파일 | 내용 |
|---|---|
| `history/history_v1.1.md` | 이전 작업(멀티 저장소 총괄 환경 구성)의 소급 기록 |
| `history/history_v1.2.md` | 이 문서 |
| `history/web/index.html` | 팀 공유용 정적 웹사이트. 버전 목록·변경 요약·접속 URL·저장소 구성을 한 페이지로 제공(외부 CDN·빌드 도구 없음) |

## 적용된 규칙 요약

| 규칙 | 적용 방법 |
|---|---|
| 한글 답변 | 기존 공통 규칙 유지 |
| DB 임의 수정 금지 | 조회만 허용. 필요한 DDL/DML은 실행하지 않고 사용자에게 제안. scheduler 로컬 기동도 사용자 확인 후 |
| 파일 삭제 보관 | `trash/<YYYY-MM-DD>/` 아래 원래 경로 구조 유지해 이동, history에 기록 |
| 자동 push 금지 | 사용자가 명시적으로 요청할 때만 push, 커밋은 `git -C <경로>`로 저장소별 |
| 버전 기록 | 파일을 바꾼 작업마다 `history/history_v<major>.<minor>.md` 추가 |
| 공유 웹사이트 | history 갱신 시 `history/web/index.html` 동시 갱신 |
| 작업 범위 | `docs/dev-environment.md`의 저장소 표에 있는 6개 저장소만 |

## 참고

- 규칙에 있던 `C:\developer\workspace\disaster-rest-harness` 경로는 이 프로젝트와 무관한 다른 저장소라 사용자 확인을 거쳐 제외했습니다.
- `history/`와 `history/web/`은 git에 커밋되는 경로입니다. 이 저장소는 public이므로 비밀번호·토큰·DB 호스트는 기록하지 않습니다.
