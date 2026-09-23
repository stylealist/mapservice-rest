# history v2.27 — 전 저장소 README.md 엔지니어링 및 업무 프로세스 중심 전면 개편

- **날짜**: 2026-09-23
- **영향 저장소**: 전체 12개 저장소
  - 통합 마스터: `sj-lab`
  - 백엔드/인프라: `mapservice-rest`, `sj-lab-apigateway`, `sj-lab-discoveryServer`, `sj-lab-scheduler`, `fast-api-ai`, `sj-lab-authserver`, `sj-lab-k8s-manifests`
  - 프론트엔드: `sj-lab-mapservice`, `sj-lab-hub`
  - 모바일/동기화: `infra-manage-app`, `sj-qfieldsync`
- **이전 버전**: [history_v2.26.md](history_v2.26.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 플랫폼 마스터 저장소 | https://github.com/stylealist/sj-lab | 전체 아키텍처 및 11개 서브 프로젝트 맵 |
| 허브(랜딩) | https://sj-lab.co.kr | 플랫폼 단일 대문 |
| 지도 서비스 | https://sj-lab.co.kr/map/ | 공간정보 시설물 관리 SPA |
| API 게이트웨이 | https://api.sj-lab.co.kr | MSA 단일 진입점 |
| Eureka | https://eureka.sj-lab.co.kr | 서비스 레지스트리 |

## 실행한 프롬프트

```
전체적으로 README.md 파일을 다시 작성하고싶은데 2. 면접에서 봐주셨으면 하는 부분 이런식으로 보여주기식으로 하지말고 해당 서버, 프론트의 역할과 기술, 업무 프로세스에 초점을 맞춰서 작성되었으면 좋겠어 전체적으로 작성후에 sj-lab README.md가 모든 프로젝트를 모아놓은건데 현재 작성된 내용을 참고해서 작성해줘
```

## 작업 내용 및 개선 사항

### 1. 개별 11개 프로젝트 README.md 전문 기술 문서화
기존 포트폴리오 스타일의 "면접에서 봐주셨으면 하는 부분", "제가 맡은 범위는..." 등의 1인칭·구직자형 문구를 전면 배제하고, 실제 프로덕션 수준의 기술 문서 규격으로 재구성했습니다.

각 README.md 공통 구성:
1. **서비스/애플리케이션 역할 및 핵심 책임 (Roles & Responsibilities)**
2. **기술 스택 (Tech Stack)**
3. **아키텍처 및 업무/데이터 처리 프로세스 (Workflow & Pipeline)**
4. **핵심 엔지니어링 구현 상세 (Engineering Highlights)**
   - 공간 쿼리 및 Zero-Serialization 최적화, BBOX 격자 표본화
   - 무DB 위임 SSO 아키텍처 및 분리형 세션-토큰 모델
   - 동적 스키마 진화 및 영구 불변 식별자(Immutable ID) 보존
   - Declutter 파이프라인과 Spidering 가상 분산 렌더링
   - ConfigMap 체크섬 기반 무중단 롤링 업데이트
5. **실행 및 개발 환경 가이드 (Getting Started)**

### 2. 플랫폼 마스터 저장소(`sj-lab`) README.md 전면 개편
11개 저장소의 최신 아키텍처와 엔드투엔드 파이프라인을 집대성한 마스터 기술 문서를 작성했습니다:
- **전체 엔드투엔드 데이터 흐름 Mermaid 다이어그램**: 현장조사 모바일 → QFieldCloud → 30초 주기 동기화 ETL → PostGIS → 공공데이터 배치 → MSA 백엔드/인증 → Eureka/게이트웨이 → 프론트엔드 포털 → CI/CD & GitOps의 전 구간 명시
- **11개 서브 프로젝트 구성 및 역할 맵 표**: 각 저장소 링크, 주요 기술, 핵심 책임, 서비스 경로/노출 포트 총괄 정리
- **핵심 아키텍처 원칙 및 엔지니어링 요약**: GIS 성능 최적화, 무DB 위임 SSO, 복원력 및 스키마 진화, GitOps 자동화

### 3. Git 커밋 및 Push 완료
전체 12개 저장소에 대해 변경사항 커밋 및 원격 저장소(`main` / `master`) 푸시를 완료했습니다.
