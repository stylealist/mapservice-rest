# history v2.57 — API 활용 페이지 4단계: 허브에서 열고 배포 준비

- **날짜**: 2026-09-29
- **영향 저장소**: `sj-lab-hub`, `sj-lab-apigateway`, `sj-lab-k8s-manifests`, `sj-lab-openapi`, `mapservice-rest`
- **이전 버전**: [history_v2.56.md](history_v2.56.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 작업 로그 |
| 허브(로컬) | http://localhost:3000 | OpenAPI 카드 열림 |
| 활용 페이지(로컬) | http://localhost:4100 | |
| 운영(배포 후) | https://sj-lab.co.kr/openapi/ | Jenkins 잡 등록 필요 |

## 실행한 프롬프트

```
내가 직접해야하는 쿼리 알려주고 커밋 푸시후에 4단계 진행해줘
openapi테이블 스키마를 api 스키마에 만들고싶어
```

## 1. 표 스키마를 `api`로 옮김 (요청)

키·사용량 표를 `map`이 아니라 **`api` 스키마**에 두도록 바꿨습니다. 공개 API 관련 표를 지도 데이터와 섞지 않기 위해서입니다.

- `db/openapi_api_key.sql`, `db/openapi_api_usage.sql` — `CREATE SCHEMA IF NOT EXISTS api;` 포함, 권한 부여 예시 주석 추가
- 스키마 이름은 설정값(`OPENAPI_DB_SCHEMA`, 기본 `api`)이라 나중에 바꿔도 코드를 고칠 필요가 없습니다.
  SQL에 그대로 들어가는 값이므로 **형식(소문자·숫자·밑줄)을 검증**한 뒤에만 씁니다.

**직접 실행하실 쿼리**는 위 두 파일 그대로입니다(재실행해도 안전). 키 표를 먼저, 사용량 표를 나중에 실행하세요.

## 2. 허브 네 번째 카드 열기

`sj-lab-hub`의 OpenAPI 카드가 "Coming Soon"에서 **바로 들어가는 카드**로 바뀌었습니다.
로컬에서는 4100, 운영에서는 `/openapi/`로 이동합니다. 포트 대응표(`LOCAL_FEATURE_PORTS`)를 하나로 모아
다음에 서비스가 늘어도 한 줄만 추가하면 됩니다.

## 3. 게이트웨이 경로 추가

`/open-api/**` → `lb://SJ-LAB-OPENAPI` 라우트를 넣었습니다. **CORS 설정은 건드리지 않았습니다** —
운영 프론트(`sj-lab.co.kr`)는 이미 허용돼 있고, 로컬은 개발 서버가 대신 호출하기 때문입니다.

## 4. 배포 준비

| 항목 | 내용 |
|---|---|
| Helm 차트 | `sj-lab-k8s-manifests/sj-lab-openapi/` 신규(기존 차트와 같은 구조). 키 기능은 기본 꺼짐 |
| Secret | 켤 때만 `openapi-db-credentials` 필요(url·username·password). 없으면 기동 실패로 바로 드러남 |
| nginx | `/openapi/` 경로를 명시해 **파일이 없으면 404**가 나게 함 — 허브 화면이 200으로 뜨던 혼란을 막음 |
| Jenkins | `docs/jenkins/sj-lab-openapi-web-pipeline.groovy` 예시 추가(스테이징 → 바꿔치기 → 확인) |

## 검증

| 확인 | 결과 |
|---|---|
| 게이트웨이(임시 8101) `/open-api/catalog` | **200** · 10KB |
| 게이트웨이 `/open-api/v1/admin-area/sido` | **200** · 2.3KB |
| 게이트웨이 `/open-api/keys/status` | **200** |
| 기존 `/map/check` | **200** (영향 없음) |
| CORS(`Origin: https://sj-lab.co.kr`) | 허용 헤더 정상 |
| 허브 카드 | OpenAPI 카드에서 "COMING SOON" 사라짐, 나머지 3개 그대로 |
| `helm lint` · `helm template` | 통과(키 기능 켠 렌더링에서 DB 환경변수 3개 확인) |
| 빌드 | 백엔드 테스트 8건 통과, 허브·활용 페이지 빌드 성공 |

검증용 게이트웨이(8101)는 확인 후 종료했고, 사용 중인 8100은 건드리지 않았습니다.

## 남은 일 (사람이 해야 하는 것)

1. **DB**: `api` 스키마에 표 2개 생성(위 스크립트).
2. **Secret**: `openapi-db-credentials` 생성 후 차트의 `apiKey.enabled: true`.
3. **Jenkins 잡 2개**: 백엔드 이미지 빌드(`sj-lab-openapi`), 활용 페이지 정적 배포(예시 파이프라인 참고).
4. **ArgoCD**: `sj-lab-openapi` 차트 Application 등록.
