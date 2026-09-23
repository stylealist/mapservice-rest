# history v2.32 — sj-lab-mapservice 소개 및 연락처 종합 정보 뷰 구현

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`, `mapservice-rest`
- **이전 버전**: [history_v2.31.md](history_v2.31.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 플랫폼 마스터 저장소 | https://github.com/stylealist/sj-lab | 전체 아키텍처 및 11개 서브 프로젝트 맵 |
| 시설물 관리 프론트 저장소 | https://github.com/stylealist/sj-lab-mapservice | 소개/연락처 뷰가 적용된 GIS 웹 정적 SPA |
| 시설물 관리 실서비스 | https://sj-lab.co.kr/map/ | [소개], [연락처] 탭 클릭 시 확인 가능 |
| 통합 랜딩 허브 | https://sj-lab.co.kr | 멀티 서비스 클라우드 허브 포털 |

## 실행한 프롬프트

```
/orchestration sj-lab-mapservice에서 소개, 연락처 부분을 채워줘
```

## 작업 내용 및 개선 사항

### 1. 소개 페이지 (About Page) 전문화 및 정보 체계 구축
단순 템플릿(문구 3줄)에 불과했던 `sj-lab-mapservice`의 소개 탭을 엔터프라이즈 GIS 포털에 걸맞은 완성도 높은 시스템 소개 뷰로 전면 개편했습니다:
- **시스템 개요 및 포지셔닝**: 현장 모바일 외업과 사무국 내업을 실시간 연결하는 OGC 표준 기반 클라우드 GIS 시설물 관리 플랫폼 규정.
- **핵심 운영 지표 (Metrics Grid)**:
  - `2,500+` 실시간 시설물 POI 객체 (BBOX 뷰포트 프리패칭 및 최적화 렌더링)
  - `30s` 모바일-클라우드 무중단 증분 동기화 주기 (QFieldCloud 변경분 감지)
  - `6단계` 내업 조치 상태 관리 (미완료·접수·처리중·완료·보류 등)
  - `100%` OGC 표준 WFS/GeoJSON 및 공공데이터(ITS CCTV/버스) 연계
- **엔드투엔드(End-to-End) 데이터 아키텍처**:
  - `Step 1 (현장 외업)`: `infra-manage-app` (QField 모바일 현장조사 및 멀티미디어 수집)
  - `Step 2 (클라우드 연동)`: `sj-qfieldsync` (Python 데몬 30초 주기 변경분 PostGIS 델타 적재)
  - `Step 3 (공간 API)`: `mapservice-rest` (Spring Boot 3 + PostGIS GiST 공간 질의 스트리밍)
  - `Step 4 (웹 관제)`: `sj-lab-mapservice` (OpenLayers 클러스터링/스파이더링 핀 표출, 내업 조치, PDF 보고서 발행)
- **주요 기능 하이라이트 (Feature Cards)**:
  - 대용량 핀 클러스터링 & Spidering 분산 알고리즘
  - 현장 멀티미디어(사진, M4A 음성, MP4 비디오) 실시간 관제 스트리밍
  - 내업 조치 기록, Canvas 이미지 자동 압축, 공공 표준 PDF 현장점검 보고서 1클릭 발행
  - 국가교통정보센터(ITS) API 및 Hls.js 기반 전국 실시간 CCTV 오버레이
  - 3단계(시·도/시·군·구/읍·면·동) 행정구역 연쇄 필터 및 정밀 공간 측정 도구
- **기술 스택 태그**:
  - Frontend: OpenLayers 7.x, Vanilla JS (ES Modules), Hls.js, jsPDF, VWorld 타일
  - Backend: Spring Boot 3.x, Java 17, PostgreSQL 15, PostGIS, HikariCP, MyBatis
  - Cloud: Kubernetes (k3s), ArgoCD GitOps, Jenkins CI/CD, Spring Cloud Gateway

### 2. 연락처 페이지 (Contact Page) 신뢰성 및 채널 일원화
형식적인 더미 연락처(02-1234-5678, 가짜 주소)를 실제 개발자 프로필 및 공식 기술 지원 창구로 전환했습니다:
- **플랫폼 리드 엔지니어 프로필**:
  - 이름: 주성중 (Sungjoong Joo)
  - 역할: SJ-LAB Project Lead · DevOps & Full-Stack Platform Engineer
  - 엔지니어링 철학 및 기술 전문 분야 소개
- **공식 지원 채널 & 저장소 링크**:
  - GitHub: `stylealist/sj-lab` (메인), `stylealist/sj-lab-mapservice` (GIS 웹), `stylealist/mapservice-rest` (공간 백엔드)
  - 기술 문의 이메일: `stylealist@naver.com`, `contact@sj-lab.co.kr` (업무일 24시간 내 회신)
  - 플랫폼 및 인프라 대시보드: SJ-LAB 허브, ArgoCD GitOps, Jenkins CI 파이프라인
- **효율적인 기술 지원 및 이슈 리포트 안내 가이드**:
  - 1. 공간 데이터 좌표 불일치 제보 요령 ('지도캡쳐' 도구 활용)
  - 2. 현장 미디어 동기화 지연 문의 가이드
  - 3. 신규 OGC 레이어 및 지자체 CCTV 연동 제안 절차

### 3. 모던 반응형 스타일시트 (`css/components/info-pages.css`) 신설
- 메인 레이아웃 및 헤더와 조화를 이루는 Slate/Blue 엔터프라이즈 테마 적용.
- 모바일(1열 세로 스택)부터 태블릿, 와이드 데스크톱(3~4열 그리드)까지 완벽한 반응형 뷰포트 지원.
- 헤더 토글 애니메이션과 연동되어 부모 높이에 맞게 완벽한 스크롤 컨테이너(`height: 100%`) 구축.

### 4. 문서 및 형상 관리 현행화
- `sj-lab-mapservice/README.md`: 소개/연락처 뷰 역할 및 디렉터리 트리에 `info-pages.css` 반영.
- `sj-lab-mapservice` 저장소 커밋 및 원격 `origin/main` 푸시 완료 (`d02209e`).
