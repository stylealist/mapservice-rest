# history v2.44 — 저장소·문의 탭 인프라 링크 3개 추가(게이트웨이·Eureka·로그인)

- **날짜**: 2026-09-28
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.43.md](history_v2.43.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 | http://localhost:4000 → 상단 "저장소 · 문의" | 이번에 손본 화면 |
| API 게이트웨이 상태 | https://api.sj-lab.co.kr/map/check | 이번에 추가 |
| Eureka | https://eureka.sj-lab.co.kr | 이번에 추가 |
| 로그인(SSO) | https://api.sj-lab.co.kr/auth/login.html | 이번에 추가 |

## 실행한 프롬프트

```
저장소 문의 플랫폼 포털 & 인프라에 https://eureka.sj-lab.co.kr Eureka,
https://api.sj-lab.co.kr/map/check api gateway, 인증 https://api.sj-lab.co.kr/auth/login.html도 추가해줘
```

## 작업 내용

"플랫폼 포털 & 인프라" 카드에 3개를 더해 **5개 → 8개**가 되었습니다. 규칙(`ui-conventions.md`)대로 **넣기 전에 셋 다 응답을 확인**했습니다.

| 추가 | 응답 |
|---|---|
| API 게이트웨이 — `https://api.sj-lab.co.kr/map/check` | `200 text/plain` · "[SJ-LAB] API 게이트웨이 및 지도/시설물 백엔드 서비스(mapservice…" |
| Eureka — `https://eureka.sj-lab.co.kr` | `200 text/html` (Eureka 대시보드) |
| 로그인(SSO) — `https://api.sj-lab.co.kr/auth/login.html` | `200 text/html` (공유 로그인 화면) |

설명은 각각 "지도·시설물 백엔드 상태 확인", "마이크로서비스 등록 현황", "허브·지도가 함께 쓰는 로그인 화면"으로 달았습니다.

### 순서도 함께 정리

링크가 8개가 되면서 **로그인 없이 바로 열리는 것 → 로그인이 필요한 것** 순으로 재배치했습니다.

```
허브 → API 게이트웨이 → Eureka → 로그인(SSO) → QFieldCloud → ArgoCD → Jenkins → 쿠버네티스 대시보드
```

카드 설명도 그 순서에 맞춰 "허브 · API 게이트웨이 · Eureka · 로그인 화면은 바로 열리고, ArgoCD · Jenkins · QFieldCloud · 쿠버네티스 대시보드는 로그인이 필요합니다"로 고쳤습니다.

## 검증 (로컬, 헤드리스 Chrome + CDP)

| 확인 | 결과 |
|---|---|
| 인프라 링크 | 8개, 순서대로 표출 |
| GitHub 저장소 · 이메일 | 13개 · `stylealist@gmail.com` 그대로 |
| 레이아웃 | 가로 스크롤 없음, 카드 높이 정상 |
| 콘솔 오류 | 없음 |

## 문서

- `docs/ui-conventions.md` — 인프라 링크 목록을 8개로 갱신하고, "로그인 없이 열리는 것 먼저" 순서 규칙과 "설명 문구도 같이 고칠 것"을 추가.
- `README.md` — 운영 중인 서비스 8곳으로 수정.

## 남은 작업

- 커밋·push.
