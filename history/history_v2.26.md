# history v2.26 — 전체 저장소 README.md 현행화 및 CLAUDE.md 유지 규칙 추가

- **날짜**: 2026-09-23
- **영향 저장소**: 전체 11개 저장소
  - 백엔드/인프라: `mapservice-rest`, `sj-lab-apigateway`, `sj-lab-discoveryServer`, `sj-lab-scheduler`, `fast-api-ai`, `sj-lab-authserver`, `sj-lab-k8s-manifests`
  - 프론트엔드: `sj-lab-mapservice`, `sj-lab-hub`
  - 모바일/동기화: `infra-manage-app`, `sj-qfieldsync`
- **이전 버전**: [history_v2.25.md](history_v2.25.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 허브(랜딩) | https://sj-lab.co.kr | 운영 서비스 첫 화면 |
| 지도 서비스 | https://sj-lab.co.kr/map/ | 운영 지도 서비스 |
| API 게이트웨이 | https://api.sj-lab.co.kr | API 단일 진입점 |
| Eureka | https://eureka.sj-lab.co.kr | 서비스 레지스트리 |
| QFieldCloud | https://qfield.sj-lab.co.kr | 모바일 현장조사 동기화 |

## 실행한 프롬프트

```
claude에서 1. 각 프로젝트에 README.md를 현재 상황에 맞게 작성해줘  2. 각 프로젝트의 claude.md에 기본설정을 하나 추가하려고하는데 프로젝트에 추가 수정되는부분이 있으면 README.md파일을 업데이트 하도록 하는 조건을 추가해줘 를진행중이었는데 토큰이 다떨어졌어 ... 모두 완료하고 commit push 진행해줘
```

## 작업 배경 및 목적

sj-lab 플랫폼이 11개 저장소로 확장되고 인증서버 신설(`sj-lab-authserver`), SSO 연동, 내업 관리 및 사진 업로드 등 대규모 기능 확장이 이루어지면서, 각 저장소의 `README.md`가 초기 템플릿 상태이거나 변경된 시스템 현실을 반영하지 못하고 있었습니다. 또한 AI 에이전트와 협업하는 과정에서 코드 변경 시 README가 누락되는 문제를 방지하기 위해 각 저장소 `CLAUDE.md`에 지속적인 문서 갱신 규칙을 명시했습니다.

## 작업 내용

### 1. 전 저장소 CLAUDE.md에 "README 유지 규칙" 추가 (11개 저장소)

모든 저장소의 `CLAUDE.md`에 아래 규칙을 표준으로 추가했습니다.
- 기능·API·화면·실행 방법·설정·배포 방식이 추가/수정되면 같은 작업에서 `README.md`도 즉시 갱신할 것
- README는 면접관·외부 리뷰어 관점에서 읽는 문서로, 구현되지 않은 기능은 과장하지 않고 한계점은 투명하게 기술할 것

### 2. 저장소별 README.md 현행화 (11개 저장소)

| 저장소 | 분류 | README 주요 갱신 내용 |
|---|---|---|
| `mapservice-rest` | 백엔드 (총괄) | WFS/QField/내업 API 구조, PostGIS 쿼리 최적화, 첨부파일 중계, 로컬 스택 실행법 |
| `sj-lab-apigateway` | 게이트웨이 | 라우팅 표(`/map`, `/auth`, `/scheduler`, `/fast-api-ai`), CORS 설정, 에러 핸들링 |
| `sj-lab-discoveryServer` | 네이밍 | Eureka 아키텍처, 로컬/운영 프로파일 차이, 인스턴스 갱신 지연 시 트러블슈팅 |
| `sj-lab-scheduler` | 데이터 배치 | 공공데이터 API 수집 주기, PostGIS 적재 및 GeoJSON 뷰 생성 흐름, CCTV 즉시 수집 |
| `fast-api-ai` | Python MSA | Spring Cloud 생태계 연동 구조, Eureka 등록 및 게이트웨이 라우팅, 향후 AI/RAG 역할 |
| `sj-lab-authserver` | 인증 | QFieldCloud 계정 위임 인증, JWT 발급 및 무DB SSO 메커니즘, 체험용 계정 안내 |
| `sj-lab-mapservice` | 프론트엔드 | 순수 정적 SPA 구조, OpenLayers GIS 시각화, 내업 관리/사진 첨부 UI, SSO 연동 |
| `sj-lab-hub` | 프론트엔드 | React 랜딩 허브, 사이트 진입점 SSO 로그인 게이트 연동, 서비스 카드 링크 구성 |
| `sj-lab-k8s-manifests` | 인프라 | 서비스별 Helm 차트 구조, ArgoCD GitOps 파이프라인, k8s Secret 주입 명세 |
| `infra-manage-app` | 모바일 앱 | QField 기반 커스텀 포크 안내, 시설물 점검 폼/미디어 필드 구성, 데이터 파이프라인 |
| `sj-qfieldsync` | 동기화 워커 | QFieldCloud 30초 주기 변경 감지, GPKG → PostGIS 동기화 알고리즘, 스키마 관리 전략 |

## 검증 및 상태

- 11개 저장소 전체 `CLAUDE.md`의 `## README 유지 규칙` 삽입 검증 완료
- 11개 저장소 전체 `README.md` 인코딩(UTF-8) 및 마크다운 문법 확인 완료
- 각 저장소별 Git 스테이징, 커밋 및 원격 브랜치 push 완료
