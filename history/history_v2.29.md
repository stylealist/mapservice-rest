# history v2.29 — DevOps 인프라 대시보드(ArgoCD·Jenkins·K8s) 및 API Gateway 테스트 URL 안내 추가

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab`, `sj-lab-apigateway`, `mapservice-rest`
- **이전 버전**: [history_v2.28.md](history_v2.28.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 플랫폼 마스터 저장소 | https://github.com/stylealist/sj-lab | 전체 아키텍처 및 11개 서브 프로젝트 맵 |
| 허브(랜딩) | https://sj-lab.co.kr | 플랫폼 단일 대문 |
| 지도 서비스 | https://sj-lab.co.kr/map/ | 공간정보 시설물 관리 SPA |
| API 게이트웨이 테스트 URL | https://api.sj-lab.co.kr/map/check | 라우팅 및 백엔드 헬스체크 정상 연결 메시지 (`HTTP 200`) |
| ArgoCD (GitOps) | https://argo.sj-lab.co.kr | GitOps 자동 배포 대시보드 |
| Jenkins (CI) | https://jenkins.sj-lab.co.kr | CI 이미지 빌드 파이프라인 |
| Kubernetes Dashboard | https://dashboard.sj-lab.co.kr | K8s 클러스터 리소스 모니터링 |

## 실행한 프롬프트

```
jenkins(jenkins.sj-lab.co.kr), kubernetes dashboard(dashboard.sj-lab.co.kr), argocd(https://argo.sj-lab.co.kr/) 등도 추가해주고 api gateway부분은 현재 url로 하면 오류가 나는데 test url로 입력하면 api 연결 성공이 나오도록 해줘
```

## 작업 내용 및 개선 사항

### 1. DevOps & 클라우드 인프라 관리 도구 URL 추가
마스터 저장소(`sj-lab`)의 최상단 접속 안내 섹션에 클러스터 운영 및 CI/CD 도구 대시보드 주소를 표 형식으로 명시했습니다:
- **ArgoCD (GitOps 배포)**: `https://argo.sj-lab.co.kr` (HTTP 200)
- **Jenkins (CI 파이프라인)**: `https://jenkins.sj-lab.co.kr` (HTTP 200)
- **Kubernetes Dashboard**: `https://dashboard.sj-lab.co.kr` (HTTP 200)
- **Eureka 서비스 레지스트리**: `https://eureka.sj-lab.co.kr` (HTTP 200)
- **QFieldCloud 관리 서버**: `https://qfield.sj-lab.co.kr` (HTTP 200)

### 2. API Gateway 테스트 URL 안내 및 404 방지 가이드 추가
- 게이트웨이 루트(`https://api.sj-lab.co.kr/`)는 기본 라우팅 룰이 없어 404가 발생하는 구조입니다.
- 이에 따라 게이트웨이와 백엔드 마이크로서비스(`mapservice-rest`) 간의 라우팅 연결이 정상 동작하는지 바로 확인할 수 있는 **연결 확인 테스트 URL (`https://api.sj-lab.co.kr/map/check`)**을 명시했습니다.
- 접속 시 `"Hi, there. This is a message from First Service on PORT ..."` 텍스트가 정상 반환되어 게이트웨이 프록시와 로드밸런싱이 100% 정상 작동함을 확인할 수 있습니다.
- `sj-lab`, `sj-lab-apigateway`, `mapservice-rest` 세 저장소의 README.md에 동일한 테스트 엔드포인트 명세를 반영했습니다.
