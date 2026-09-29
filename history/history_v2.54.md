# history v2.54 — API 활용 페이지 1단계: 공개 API 서비스 만들기

- **날짜**: 2026-09-29
- **영향 저장소**: `sj-lab-openapi`(신규), `sj-lab-openapi-web`(신규·빈 저장소), `mapservice-rest`(문서·기록)
- **이전 버전**: [history_v2.53.md](history_v2.53.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 작업 로그 |
| 새 저장소(백엔드) | https://github.com/stylealist/sj-lab-openapi | 공개 API 서비스 |
| 새 저장소(프론트) | https://github.com/stylealist/sj-lab-openapi-web | 활용 페이지(다음 단계) |
| 로컬 확인 | `http://localhost:8110/open-api/catalog` | 검증용 임시 포트 |

## 실행한 프롬프트

```
rest api로 활용 가능한 부분을 추려서 api 활용 페이지를 만들고 싶은데 sj-lab-hub에 4번째에
OpenApi에 위치하도록 해주고 프론트는 sj-lab-hub처럼 react로 만들어주고 백엔드는 springboot로
만들어서 만들어 줄 수 있어?
github 저장소 만들어줘
그리고 1단계 부터 진행해줘
```

## 무엇을 만들었나

우리가 모아 둔 지도·시설물 데이터를 **밖에서도 쓸 수 있게 열어 주는 서비스**입니다. 사람이 읽을 문서와
프로그램이 부를 주소를 따로 관리하면 금방 어긋나서, **한 파일에서 둘 다 나오게** 만들었습니다.

| 주소 | 하는 일 |
|---|---|
| `GET /open-api/catalog` | 열어 둔 API 목록·설명·파라미터·예시를 통째로 내려줌 → 활용 페이지가 이걸로 화면을 그림 |
| `GET /open-api/v1/...` | 요청을 검사해 `mapservice-rest`에서 데이터를 받아 그대로 전달 |

### 열어 둔 API 12개 (전부 조회)

| 묶음 | 내용 |
|---|---|
| 공공데이터 6종 | 편의점 · 버스정류장 · CCTV · 약국 · 병원 · 관공서 (`bbox`로 화면 범위만) |
| 시설물 3종 | 목록 · 상세 · 아이콘 설정 |
| 행정구역 3종 | 시·도 → 시·군·구 → 읍·면·동 |

**닫아 둔 것**: 내업 기록 쓰기, 내업 사진, 첨부 파일(사진·음성·영상) 중계. 쓰기이거나 외부 계정이 필요해서입니다.

### 두 가지 원칙

1. **공개 범위는 파일 하나(`api-catalog.json`)가 정합니다.** 문서와 허용 목록이 같은 파일이라 "문서엔 있는데
   실제론 안 되는 API"가 생기지 않습니다. 새 API를 열 때도 이 파일만 고치면 됩니다.
2. **데이터베이스를 직접 읽지 않습니다.** 같은 SQL이 두 저장소에 생기면 한쪽만 고쳐져 답이 달라지므로,
   데이터는 항상 `mapservice-rest`에서 받아옵니다.

## 검증 (로컬, 포트 8110)

| 요청 | 결과 |
|---|---|
| API 목록 | 200 · 10KB · 그룹 3개 / 엔드포인트 12개 |
| 시·도 목록 | 200 · 2.3KB |
| 시·군·구(`sidoCd=11`) | 200 · 3.3KB |
| 편의점(`bbox` + `limit=20`) | 200 · 8KB · GeoJSON |
| 시설물(`sidoCd=11`) | 200 · 124KB |
| 시설물 상세(실제 ID) | 200 · 없는 ID는 404 그대로 전달 |
| 아이콘 설정 | 200 · 2.5KB |

**막아야 하는 요청도 확인했습니다.**

| 요청 | 결과 |
|---|---|
| 목록에 없는 경로 | 404 `UNKNOWN_API` |
| 모르는 파라미터(`?secret=1`) | 400 `UNKNOWN_PARAMETER` |
| 내업 쓰기 경로 | 404 |
| 첨부 파일 중계 경로 | 404 |

테스트 4건(카탈로그 형식·경로 중복·경로 변수 매칭)도 통과했습니다.

## 바뀐 파일

| 저장소 | 내용 |
|---|---|
| `sj-lab-openapi` | Spring Boot 프로젝트 전체 — 카탈로그(`api-catalog.json`), 중계(`ApiProxyService`), 오류 응답, 테스트, Dockerfile, README, CLAUDE.md |
| `mapservice-rest` | `docs/dev-environment.md`(저장소·포트 표), `docs/system-architecture.md`(계층·라우트·API 계약), 작업 범위 등록 |

## 남은 단계

2. 활용 페이지(React) — API 목록, 직접 실행해 보기, 샘플 코드
3. API 키 발급·사용량 (DB 스크립트는 제가 만들고 실행은 담당자)
4. 허브 4번째 카드 열기 · 게이트웨이 라우트·CORS · 차트 · nginx 경로 · 배포
