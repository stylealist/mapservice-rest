# history v2.5 — scheduler 기동 시 CCTV 데이터 1회 자동 수집

- **날짜**: 2026-09-22
- **영향 저장소**: `sj-lab-scheduler`(백엔드 신규 파일), `mapservice-rest`(문서만)
- **이전 버전**: [history_v2.4.md](history_v2.4.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| CCTV 수동 트리거(참고) | `POST/GET /scheduler/its/cctv-info` | 게이트웨이 경유 |

## 실행한 프롬프트

```
/orchestration cctv레이어는 주기적으로 1번씩 돌려줘야 영상이 나와 그래서 서버를 기동할떄 scheduler 서버를 기동할떄 cctv레이어는 한번 받도록 해줘
```

## 상황

CCTV 레이어는 스트리밍 URL이 주기적으로 갱신되어야 재생됩니다. 기존에는 `ItsDataSchedulerController.cctvInfo()`가 매일 06:00 cron으로만 실행돼, scheduler 서버를 그 시각 이후에 새로 띄우면 다음 날 06:00까지 CCTV 데이터가 비어 있거나 오래된 상태로 남아 있었습니다.

## 변경된 결과물

### 백엔드 (`sj-lab-scheduler`)

| 파일 | 내용 |
|---|---|
| `src/main/java/com/example/scheduler/config/CctvStartupRunner.java` (신규) | `ApplicationReadyEvent`를 구독해 서버 기동 직후 `ItsDataSchedulerController.cctvInfo()`를 1회 실행. 기동을 막지 않도록 별도 스레드에서 실행하며, `@Transactional`이 cron 트리거 때와 동일하게 적용되도록 self-invocation이 아닌 **주입받은 프록시 빈**을 통해 호출 |
| `CLAUDE.md` | 도메인별 스케줄 표의 CCTV 행에 기동 시 1회 자동 실행 사실 추가 |

기존 `cctvInfo()`(매일 06:00 cron + 수동 트리거 `/its/cctv-info`)는 그대로 유지하고, 서버 기동 시점에 한 번 더 실행되도록 한 것만 추가했습니다. 실패해도 `cctvInfo()` 내부에서 예외를 삼키므로(`e.printStackTrace()`) 서버 기동 자체에는 영향이 없습니다.

### 문서 (`mapservice-rest`)

| 파일 | 내용 |
|---|---|
| `docs/dev-environment.md` | scheduler 항목에 "CCTV는 기동 시 1회 즉시 수집됨" 비고 추가 |

## 검증 결과

- `sj-lab-scheduler`를 JDK 17로 `mvnw.cmd compile` 정상 컴파일 확인.
- 로컬 스택에 scheduler가 포함되어 있지 않아(문서에 따라 DB 적재 우려로 기본 기동 대상 제외) 실제 기동 후 CCTV 데이터 적재까지는 검증하지 못했습니다. scheduler를 기동해 실제 DB에 적재하는 것은 사용자 확인 후 진행 필요.
