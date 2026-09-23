# history v2.22 — 지도에서 허브로 돌아가는 링크 추가

- **날짜**: 2026-09-23
- **영향 저장소**: `sj-lab-mapservice`(프론트), `mapservice-rest`(기록)
- **이전 버전**: [history_v2.21.md](history_v2.21.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 헤더 오른쪽 "허브" 버튼 |
| 로컬 허브 | http://localhost:3000 | 링크 대상(로컬) |
| 운영 지도 → 허브 | https://sj-lab.co.kr/map/ → https://sj-lab.co.kr/ | 링크 대상(운영) |

## 실행한 프롬프트

```
mapservice에서 hub로 돌아갈수가없는데 돌아갈수있도록 할 수 있어?
```

## 작업 내용

허브(`sj-lab-hub`)에서 지도로 들어오는 경로는 있었지만 **되돌아갈 방법이 없었습니다.** 헤더에 허브 링크를 추가했습니다.

| 항목 | 파일 | 내용 |
|---|---|---|
| 링크 추가 | `index.html` | 헤더 오른쪽 네비 앞에 `#hubLink`(`.hub-link`, 격자 아이콘 + "허브"). **`.nav-btn`을 쓰지 않음** — `ui.js`가 `.nav-btn` 전부를 페이지 전환 버튼으로 바인딩하므로 그 클래스를 붙이면 허브로 못 가고 페이지 전환이 됨 |
| 환경 분기 | `index.html` 하단 인라인 스크립트 | 기본 `href="/"`(운영은 같은 오리진 루트), `localhost`/`127.0.0.1`이면 `http://localhost:3000`으로 교체. `sj-lab-hub`의 `resolveFeaturePath()`와 짝 |
| 스타일 | `css/components/header.css` | 네비 탭과 구분되는 테두리형 알약 버튼, 좁은 화면에서는 아이콘만 표시 |
| 캐시 무효화 | `index.html` | `header.css?v=20260922-2` → `?v=20260923` |
| 문서 | `sj-lab-mapservice/CLAUDE.md` | 헤더 구성 설명에 허브 링크와 "`.nav-btn`을 붙이지 말 것" 규칙 추가 |

로고(`#logoHome`)는 이미 지도 페이지로 돌아가는 동작에 쓰이고 있어 건드리지 않았습니다.

## 검증 (로컬, 헤드리스 Chrome + CDP)

| 확인 | 결과 |
|---|---|
| 헤더 표시 | "허브" 버튼이 지도·소개·연락처 왼쪽에 표시(캡처 확인) |
| 로컬 링크 | `href = http://localhost:3000/` |
| 네비 바인딩 | `.nav-btn` 아님 → 페이지 전환 핸들러에 걸리지 않음 |
| 운영 링크 | 마크업 기본값 `/` (같은 오리진 루트 = 허브) |

## 남은 작업

- 커밋·push 후 운영에서 지도 → 허브 이동 육안 확인.
