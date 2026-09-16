# history v1.3 — 변경 이력 공유 웹사이트 발행

- **날짜**: 2026-09-16
- **기준 저장소**: `C:\developer\workspace\mapservice-rest` (총괄 허브)
- **이전 버전**: [history_v1.2.md](history_v1.2.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 프론트엔드 | http://localhost:4000 | 로컬 스택 기동 후 |
| API Gateway | http://localhost:8100/map/admin-area/sido | 게이트웨이 경유 API 확인 |
| Eureka 대시보드 | http://localhost:8761 | 서비스 등록 상태 |

## 실행한 프롬프트

```
이런식으로 웹사이트에서 변경내역을 볼 수 있으면 좋겠어
https://claude.ai/code/artifact/8a2480c7-2c72-4649-831a-417981b9f4a1 이런식으로 해달라는거야
```

기존 `history/web/index.html`은 로컬 파일이라 팀원에게 링크로 공유할 수 없었습니다. 예시로 주신 페이지(`disaster-rest 리팩터링 로그`)와 같은 **버전 전환기 + 버전별 패널** 구조로 다시 만들어 발행했습니다.

## 변경된 결과물

| 파일 | 구분 | 내용 |
|---|---|---|
| `history/web/artifact.html` | 신규 | 발행되는 공유 페이지의 원본. 상단 고정 버전 전환기 + v1.0~v1.3 패널 4개 |
| `history/web/index.html` | 수정 | 맨 위에 발행 페이지 주소 안내 추가(오프라인 요약 페이지로 유지) |
| `history/history_v1.0.md` ~ `v1.2.md` | 수정 | 접속 URL 표 맨 위에 발행 페이지 주소 추가 |
| `history/history_v1.3.md` | 신규 | 이 문서 |
| `CLAUDE.md` | 수정 | history 갱신 시 발행 페이지도 함께 갱신하는 규칙 추가 |

## 발행 페이지 구성

- **버전 전환기** — 상단 고정. 버전 칩과 이전·다음 버튼, 최신 표시 점, 현재 위치(`v1.3 · 4/4`) 표시
- **버전별 패널** — 머리말(요약 수치) → 실행한 프롬프트 원문 → 변경 결과물 표 → 설계 결정 → 검증 결과
- **프롬프트 원문 보존** — "무엇을 시켰고 무엇이 나왔는지"를 한 화면에서 대조
- **미해결 항목 구분** — 확인한 것과 확인하지 못한 것을 나눠 표시
- **테마 대응** — 보는 사람의 라이트·다크 설정에 맞춰 표시
- 색·서체는 지도 서비스에 맞춰 남색 계열 + `Noto Serif KR`(제목) / `IBM Plex Sans KR`(본문) / `JetBrains Mono`(수치·코드)

## 운영 방법

- 발행 페이지는 **기본 비공개**입니다. 팀에 공유하려면 페이지의 공유 메뉴에서 링크를 열어야 합니다.
- 다음부터 history를 추가할 때는 `history/web/artifact.html`에 패널을 추가하고 같은 주소로 다시 발행하면 링크가 유지됩니다.
- 민감 정보(비밀번호·토큰·DB 호스트)는 발행 페이지에 싣지 않습니다. 발행은 외부 서비스에 내용을 올리는 행위이므로 기록 범위를 넓힐 때 한 번 더 확인합니다.
