# history v2.36 — 소개·연락처 탭 문구를 사람 말투로 고치고 연락처 링크 정리

- **날짜**: 2026-09-28
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.35.md](history_v2.35.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 소개 탭 | http://localhost:4000 → 상단 "소개" | 이번에 손본 화면 |
| 로컬 연락처 탭 | http://localhost:4000 → 상단 "연락처" | |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 후 확인 |

## 실행한 프롬프트

```
소개, 연락처탭의 설명들이 너무 기계적으로 딱딱해 자연스러운 단어 위주로 구사해서 ai같지않도록 해줘
그리고 연락처탭에서 GitHub 저장소를 더 추가해주고 이메일에서 contact@sj-lab.co.kr제거,
stylealist@naver.com는 stylealist@gmail.com으로 변경해줘
플랫폼 포털 & 인프라에는 qfield, kubernetes도 추가해줘
```

## 작업 내용

### 1. 문구 전면 수정 (43곳)

홍보 문구와 번역투를 걷어내고, 무엇이 어떻게 동작하는지를 그대로 적었습니다.

| 전 | 후 |
|---|---|
| 현장 점검부터 사무실 조치까지, 원스톱 공간정보(GIS) 시설물 관리 플랫폼 | 현장에서 찍은 사진 한 장이 사무실의 조치 기록까지 이어지도록 |
| 차세대 클라우드 GIS 웹 서비스 … 엔드투엔드로 완결합니다 | 현장에 나간 조사원이 앱으로 남긴 위치와 사진을, 사무실에서 지도를 열면 그대로 볼 수 있게 만든 서비스입니다 |
| 엔드투엔드(End-to-End) 데이터 아키텍처 | 데이터가 지나가는 길 |
| 주요 기능 및 엔지니어링 하이라이트 | 이런 기능들이 있습니다 |
| 클러스터링 & Spidering 분산 / 지능적으로 그룹화 · 100% 동기화 · 정밀 선택성을 보장 | 핀이 겹칠 때 묶고, 확대하면 펼치기 / 묶음에 적힌 숫자는 옆 목록의 건수와 늘 같게 맞췄습니다 |
| 적용 기술 스택 (Technology Stack) | 사용한 기술 |
| 시스템 문의 및 플랫폼 기술 지원 | 궁금한 점이나 고칠 부분이 있다면 |
| Platform Architect & Lead Engineer | 만든 사람 |
| 효율적인 기술 지원 및 이슈 리포트 안내 | 이런 식으로 알려 주시면 빠릅니다 |

인트로 팝업 안의 소개 문단·폴백 카드 문구도 같은 톤으로 맞췄습니다(팝업만 옛 문투로 남지 않도록).

### 2. 틀린 내용 바로잡기

문구를 고치면서 실제 코드와 어긋난 부분을 확인해 함께 고쳤습니다.

| 항목 | 전 | 후 | 근거 |
|---|---|---|---|
| 내업 처리 상태 | 6단계 | **5단계** | `PENDING`/`RECEIVED`/`IN_PROGRESS`/`DONE`/`HOLD` |
| 지표 4번째 | 100% OGC 표준 & 공공데이터 | **6종** 함께 보는 공공데이터 | 편의점·버스정류장·CCTV·약국·병원·관공서 |
| PDF 생성 | 프론트 `jsPDF` 뱃지 | **OpenPDF (보고서 생성)** 로 교체해 Backend 그룹으로 | 보고서는 `mapservice-rest`의 `QfieldReportController`가 OpenPDF로 생성 |
| DB 버전 | PostgreSQL 15 | **PostgreSQL 17 · PostGIS 3.4** | 운영 DB 실제 버전 |
| 동기화 대상 | PostGIS `infra_facilities` 테이블 | PostGIS **`qfield` 스키마** | `sj-qfieldsync`가 적재하는 실제 위치 |

### 3. 연락처 링크 정리

- **GitHub 저장소 3개 → 13개**: `sj-lab`, `sj-lab-hub`, `sj-lab-mapservice`, `mapservice-rest`, `sj-lab-apigateway`, `sj-lab-discoveryServer`, `sj-lab-scheduler`, `sj-lab-authserver`, `sj-qfieldsync`, `sj-qfieldCloud`, `fast-api-ai`, `sj-lab-k8s-manifests`, `sj-lab-nginx`. GitHub API로 **공개 저장소만** 확인해 넣었고(비공개 1개는 제외), 링크마다 무슨 저장소인지 한 줄(`.link-note`)을 붙였습니다.
- **이메일**: `contact@sj-lab.co.kr` 제거, `stylealist@naver.com` → **`stylealist@gmail.com`**.
- **플랫폼 포털 & 인프라**: 기존 허브·ArgoCD·Jenkins에 **QFieldCloud**(https://qfield.sj-lab.co.kr)와 **Kubernetes (k3s)** 추가. 쿠버네티스 대시보드는 NodePort(32443)로만 열려 있어 외부 주소가 없으므로, 없는 주소를 적는 대신 **배포 매니페스트 저장소**를 링크하고 "대시보드는 클러스터 안에서만"이라고 밝혔습니다.

### 4. 레이아웃 (`css/components/info-pages.css`)

- 연락처 카드 그리드를 3열 → **2열**로 바꾸고, 저장소 카드만 `.contact-channel-card.wide`로 한 줄 전체를 쓰게 했습니다.
- `.channel-links.repo-list` — 폭이 되는 만큼 여러 열로 배치(`auto-fill, minmax(330px, 1fr)`). `word-break: break-all` 때문에 `sj-lab-k8s-man / ifests`처럼 글자 단위로 끊기던 것을 `keep-all` + `overflow-wrap`으로 고쳤습니다.
- `.link-note`(링크 옆 회색 설명) 추가. `.channel-desc`의 `flex-grow: 1`을 빼서 카드 중간에 빈 공간이 생기지 않게 했습니다.
- 참조 버전 `info-pages.css?v=20260928`.

## 검증 (로컬, 헤드리스 Chrome + CDP, 1440×1000)

| 확인 | 결과 |
|---|---|
| 소개 탭 제목·지표 | "현장에서 찍은 사진 한 장이 / 사무실의 조치 기록까지 이어지도록", 지표 4개(2,500+ · 30초 · 5단계 · 6종) |
| 기능 카드 · 기술 뱃지 | 6개 / 18개 정상 표출 |
| 연락처 링크 | GitHub 13 + 배포 매니페스트 1, 메일 `mailto:stylealist@gmail.com` 하나, 인프라 4개(허브·ArgoCD·Jenkins·QFieldCloud) |
| 카드 폭 | 저장소 카드 1331px(전체 폭), 이메일·인프라 656px씩. 가로 스크롤 없음 |
| 남은 문구 검사 | `naver.com` · `contact@sj-lab.co.kr` · `jsPDF` · `PostgreSQL 15` · `차세대` · `100% 동기화` 모두 제거 확인 |
| 콘솔 오류 | 없음 |

## 문서

- `docs/ui-conventions.md` — 소개·연락처 문구 톤 규칙(홍보 문구·번역투 금지, 숫자는 실제 값), 기술 뱃지는 실제 쓰는 것만, 저장소 목록은 수동 관리(공개 저장소만), 없는 주소 금지를 "반드시 지킬 것"에 추가.
- `README.md` — 소개·연락처 화면 설명을 현재 내용(공개 저장소 13곳, 운영 서비스 링크)으로 갱신.

## 남은 작업

- 커밋·push 여부 확인.
- 운영 배포 후 실제 브라우저에서 연락처 링크(특히 QFieldCloud·매니페스트) 확인.
