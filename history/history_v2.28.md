# history v2.28 — sj-lab 마스터 README에 실서비스 접속 안내 및 엔드투엔드 파이프라인 가시화 강화

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab`, `mapservice-rest`
- **이전 버전**: [history_v2.27.md](history_v2.27.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 플랫폼 마스터 저장소 | https://github.com/stylealist/sj-lab | 전체 아키텍처 및 11개 서브 프로젝트 맵 |
| 허브(랜딩) | https://sj-lab.co.kr | 플랫폼 단일 대문 (React 18 SPA) |
| 지도 서비스 | https://sj-lab.co.kr/map/ | 공간정보 시설물 관리 SPA (OpenLayers 7) |
| 중앙 인증 서버 | https://api.sj-lab.co.kr/auth/login.html | 데모 원클릭 로그인 지원 |

## 실행한 프롬프트

```
sj-lab에서 1. 플랫폼 개요 및 엔드투엔드(End-to-End) 데이터 흐름 부분이 거의 안보여 그리고 현재 sj-lab-hub,sj-lab-mapservice에 대한 설명이 sj-lab 메인 README.md에 없어서 실제 웹사이트를 어디로 들어가서 확인하는지 알수가없어
```

## 작업 내용 및 개선 사항

### 1. 실서비스 접속 링크 및 원클릭 체험 안내 최상단 전진 배치
- `https://sj-lab.co.kr` (랜딩 허브), `https://sj-lab.co.kr/map/` (지도 서비스), `https://api.sj-lab.co.kr` (게이트웨이) 등의 운영 주소를 표 형식으로 명시.
- 로그인 화면의 **`[체험용 계정으로 로그인]` 버튼**을 통해 회원가입이나 계정 입력 없이 즉시 데모 토큰을 발급받아 전체 서비스를 확인할 수 있는 1초 가이드를 최상단에 추가.

### 2. 핵심 웹 서비스(`sj-lab-hub`, `sj-lab-mapservice`) 상세 안내 섹션 신설
- **`sj-lab-hub`**: React 18 기반 런치패드, React 번들 파싱 전 선제적 SSO 인증 게이트웨이(Zero-FOUC), 로컬/운영 동적 환경 분기 라우팅 설명.
- **`sj-lab-mapservice`**: 약 2,500건 시설물 공간정보 시각화, 3단계 행정구역 연쇄 필터, 클러스터링 및 고배율 Zoom 18 Spidering 가상 분산, 상세 팝업 및 현장 음성/사진/동영상 스트리밍, 내업(사무실 조치) 및 PDF 보고서 생성, 전국 실시간 CCTV 스트리밍 오버레이 등 주요 기능과 화면 구성을 상세히 기술.

### 3. 엔드투엔드 데이터 흐름 가시성 강화
- GitHub 마크다운 렌더링 시 축소되어 가독성이 떨어지던 다이어그램 문제를 해결하기 위해, 5개 영역으로 구획된 **선명한 박스형 텍스트 파이프라인 흐름도**를 전면에 배치.
- 현장 모바일 점검 → 30초 주기 증분 ETL → PostGIS 공간 뷰 결합 → 게이트웨이 및 웹 지도 표출 → 사무실 조치(내업) 및 피드백에 이르는 **5단계 상세 업무 시나리오**를 기술.
