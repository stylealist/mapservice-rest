# history v1.9 — 서비스 이름을 "SJ 시설물 관리"로 변경

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-lab-mapservice`(지도 서비스), `sj-lab-hub`(첫 화면)
- **이전 버전**: [history_v1.8.md](history_v1.8.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 이름 변경 확인 |
| 첫 화면 허브(로컬) | `npm start` 후 http://localhost:3000 | 검증은 3010 포트로 임시 기동 후 종료 |

## 실행한 프롬프트

```
원래 기존에 이름이 SJ Map Platform 이었는데 지금은 sj에서 시설물관리하는 기능인데 hub, sj-lab-mapservice에서 적절한 이름으로 변경해줄수있어?
```

확인 질문에 대한 사용자 선택:
- 서비스명: **"SJ 시설물 관리"** (기존 SJ 브랜드 유지 + 기능 명시)
- 허브 첫 카드: **아이콘만 교체**(경로 `/map`은 유지 — 배포·nginx 설정에 영향 없음)

## 변경된 결과물

| 저장소 | 파일 | 변경 |
|---|---|---|
| `sj-lab-mapservice` | `index.html` | 브라우저 탭 제목, 화면 좌측 상단 로고(`#logoHome`) → **SJ 시설물 관리** |
| `sj-lab-mapservice` | `js/app.js` | 초기화 완료 콘솔 로그 문구 |
| `sj-lab-mapservice` | `README.md` | 제목과 소개문 — "웹 기반 지도 서비스" → "시설물 관리 서비스(주차장·강당·강의실·충전소·체육시설 등)" |
| `sj-lab-mapservice` | `.claude/agents/reviewer.md` | 리뷰어 에이전트 설명의 프로젝트명 |
| `sj-lab-hub` | `src/App.js` | 첫 카드 제목 "2D 지도" → **"시설물 관리"**, 설명 "Interactive Maps" → "Facility Management", 아이콘 🗺️ → 🏢 |
| `sj-lab-hub` | `CLAUDE.md` | 프로젝트 개요의 기능 목록 표기 |

경로(`/map`), API 주소, Eureka 서비스명, 저장소 이름은 **바꾸지 않았습니다** — 배포·라우팅에 영향을 주기 때문입니다.

## 검증 결과

Chrome으로 두 화면을 모두 확인했습니다.

| 확인 | 결과 |
|---|---|
| 지도 서비스 탭 제목 | `SJ 시설물 관리` |
| 지도 서비스 로고 | `SJ 시설물 관리` |
| 허브 카드 목록 | `시설물 관리`, `3D 가시화`, `Lab`, `OpenAPI` |
| 허브 첫 카드 | 🏢 아이콘 + "Facility Management" |
| 콘솔 오류 | 지도 0건 / 허브 1건(정적 리소스 404 — 이번 변경과 무관한 기존 현상) |

허브는 검증용으로 3010 포트에 임시 기동한 뒤 종료했습니다(사용자의 기본 포트 3000은 건드리지 않음).

## 참고

- 실서버 반영은 각 저장소를 push해 Jenkins → ArgoCD 경로로 배포되어야 보입니다.
- 커밋·push는 하지 않았습니다.
