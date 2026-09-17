# history v1.31 — sj-lab-hub 탭 아이콘(favicon)을 허브 전용 모양으로 신규 제작

- **날짜**: 2026-09-17
- **영향 저장소**: `sj-lab-hub`
- **이전 버전**: [history_v1.30.md](history_v1.30.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 허브 개발 서버 | http://localhost:3000 | `npm start` 후 탭 아이콘 확인 |
| 허브 탭 아이콘 파일 | http://localhost:3000/favicon.svg | 빌드 결과에서는 `build/favicon.svg` |

## 실행한 프롬프트

```
sj-lab-hub 탭 아이콘은 동일한 아이콘이 아니라 sj-lab-hub에 맞는 아이콘 모양으로 신규로 만들어서 반영해줘
```

## 원인

`public/index.html`에 탭 아이콘 지정이 없어 브라우저 기본 아이콘(빈 아이콘)이 보였습니다.

## 디자인

허브는 기능 카드 4개로 들어가는 첫 화면이므로 **"네 기능으로 들어가는 입구"** 를 아이콘으로 표현했습니다.

- 배경: 허브 화면 배경과 같은 슬레이트 그라데이션(`#475569` → `#1e293b`) 둥근 사각형
- 타일 2×2: `src/App.js` `features` 카드의 그라데이션 시작색 그대로
  - 시설물 관리 `#3b82f6`(파랑) · 3D 가시화 `#10b981`(초록)
  - Lab `#f59e0b`(주황) · OpenAPI `#8b5cf6`(보라)
- 지도 서비스의 파란 집 모양 아이콘과 나란히 떠 있어도 한눈에 구분됩니다.

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `public/favicon.svg` | **신규** — 위 디자인의 32×32 SVG |
| `webpack.config.js` | `HtmlWebpackPlugin`에 `favicon: "./public/favicon.svg"` 추가 — 빌드 시 `build/`로 복사하고 `<link rel="icon">` 자동 주입 |
| `CLAUDE.md` | 탭 아이콘 규칙 추가 — 카드 추가·색 변경 시 함께 고칠 것, 템플릿에 link를 직접 넣지 말 것 |

- `public/index.html`에 link를 직접 넣지 않은 이유: 이 저장소는 `public/`을 개발 서버에서만 정적으로 서빙하고 **빌드 결과(`build/`)로는 복사하지 않습니다.** 템플릿에만 넣으면 배포본에서 아이콘 파일이 404가 됩니다. `favicon` 옵션은 파일 복사와 링크 주입을 함께 해 줍니다.

## 검증 결과

- `npm run build` 성공 — `build/favicon.svg`(821B) 생성, `build/index.html`에 `<link rel="icon" href="favicon.svg">` 주입 확인
- 빌드 결과를 임시 포트 3011로 서빙해 Chrome에서 확인(기본 3000 포트는 사용하지 않음, 확인 후 종료)
  - 아이콘 200 `image/svg+xml`, 카드 4개 정상 렌더링, 콘솔 오류 0건
  - 16·32·64px로 밝은/어두운 탭 배경 위에서 지도 서비스 아이콘과 나란히 비교 — 16px에서도 네 색 타일이 구분됨

## 참고

- 어두운 탭 배경에서는 슬레이트 배경의 대비가 낮지만, 색 타일이 뚜렷해 식별에는 문제가 없습니다.

## 커밋·푸시

후속 요청 "커밋하고 푸시해줘"에 따라 두 저장소를 커밋하고 `origin/main`에 푸시했습니다.

| 저장소 | 커밋 |
|---|---|
| `sj-lab-hub` | 탭 아이콘(favicon)을 허브 전용 모양으로 추가 |
| `mapservice-rest` | history v1.31 + 공유 페이지 |
