# history v2.31 — SJ-LAB 마스터 플랫폼 정의 및 다중 서비스 확장 맵 개편

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab`, `mapservice-rest`
- **이전 버전**: [history_v2.30.md](history_v2.30.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 플랫폼 마스터 저장소 | https://github.com/stylealist/sj-lab | 전체 아키텍처 및 11개 서브 프로젝트 맵 |
| 통합 랜딩 허브 | https://sj-lab.co.kr | 다양한 서비스 런치패드 (React 18 SPA) |
| 시설물 관리 지도 | https://sj-lab.co.kr/map/ | [플래그십] 공간정보 시설물 관리 SPA |
| API 게이트웨이 테스트 | https://api.sj-lab.co.kr/map/check | 라우팅 및 헬스체크 한글 응답 (`HTTP 200`) |

## 실행한 프롬프트

```
sj-lab README에 SJ-LAB: Kubernetes & MSA 기반 공간정보(GIS) 및 시설물 관리 통합 플랫폼 이렇게 설명이 되어있는데 이번에 만든게 시설물 관리인거지 sj-lab-hub 보면 알겠지만 다양한 프로젝트를 넣을거야
```

## 작업 내용 및 개선 사항

### 1. SJ-LAB 플랫폼의 본질과 비전 재정립
- **기존 한계**: 전체 플랫폼이 오직 '시설물 관리'만을 위한 단일 프로젝트인 것처럼 표현되었던 부분을 수정.
- **재정립된 플랫폼 정의**:
  `SJ-LAB`은 Naver Cloud VPC 위의 Kubernetes 클러스터와 MSA(Spring Cloud, Netflix Eureka) 아키텍처를 기반으로, **공간정보(GIS), 3D 가시화, AI/빅데이터 예측 모델, OpenAPI 등 다양한 도메인의 웹 서비스와 실험적 기술 프로젝트를 유연하게 탑재하고 확장하는 종합 클라우드 웹 플랫폼**으로 위상을 확립했습니다.
- "시설물 관리 2D GIS"는 플랫폼 위에 가장 먼저 완벽히 구축된 **첫 번째 핵심 플래그십 프로젝트**로 규정했습니다.

### 2. 플랫폼 서비스 맵 & 확장 아키텍처 다이어그램 추가
중앙 대문인 `sj-lab-hub`의 카드 구조와 연계하여 서비스 도메인을 체계화했습니다:
1. 🗺️ **시설물 관리 2D (GIS 플랫폼)**: [운영 중 / 플래그십] 모바일 현장조사 ~ PostGIS ~ 내업 관리 지도 SPA
2. 🧊 **3D 공간 시뮬레이션 (Digital Twin)**: [확장 예정] Three.js, Cesium 지형분석, 3D BIM/건물 모델링
3. 🧪 **AI / 데이터 실험실 (Predictive Lab)**: [확장 예정] 시계열 예측(LSTM), 음성 메모 STT 요약, RAG 지식검색
4. 🌐 **OpenAPI & 데이터 허브**: 플랫폼 데이터 개방 및 연계
