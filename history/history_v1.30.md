# history v1.30 — 웹사이트 탭 아이콘(favicon)을 메인 로고로 변경 + v1.28~v1.29 커밋·푸시

- **날짜**: 2026-09-17
- **영향 저장소**: `sj-lab-mapservice`(프론트엔드), `mapservice-rest`(history)
- **이전 버전**: [history_v1.29.md](history_v1.29.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 프론트엔드 | http://localhost:4000 | 브라우저 탭 아이콘 확인 (캐시 때문에 안 바뀌면 Ctrl+F5) |
| 탭 아이콘 파일 | http://localhost:4000/images/favicon.svg | |

## 실행한 프롬프트

```
커밋 및 푸시해주고 추가로 웹사이트 탭의 아이콘을 아무것도 없는상태가 아니라 우리 메인로고인 <span class="logo-mark" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M5 20V8.6L12 4.5l7 4.1V20" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"></path>
              <path d="M9.4 20v-4.4h5.2V20" stroke="currentColor" stroke-width="1.6"></path>
              <path d="M3.2 20h17.6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
            </svg>
          </span>로 변경해줘
```

## 원인

`index.html`에 `<link rel="icon" href="data:," />`(빈 아이콘)가 들어 있어 탭 아이콘이 비어 보였습니다. 독립 팝업 페이지(로드뷰·지도 편집기)에는 아이콘 지정이 아예 없었습니다.

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `images/favicon.svg` | **신규** — 헤더 로고 배지(`.logo-mark`)와 같은 모양: 파란 그라데이션(`#2563eb`→`#1d4ed8`) 둥근 사각형 + 흰색 로고 글리프(요청한 path 3개 그대로) |
| `index.html` | 빈 아이콘(`data:,`) → `images/favicon.svg` |
| `html/loadview/load-view.html` | 탭 아이콘 추가(`../../images/favicon.svg`) |
| `html/fabric/fabric-editor.html` | 탭 아이콘 추가(`../../images/favicon.svg`) |
| `docs/ui-conventions.md` | 탭 아이콘 규칙 추가 — 로고를 바꾸면 이 파일도 함께 고칠 것 |

- 로고 글리프만 넣으면 `currentColor`가 없어 검게 그려지고, 투명 배경이라 어두운 탭에서 안 보입니다. 그래서 **헤더에서 보이는 배지 모양 그대로**(파란 배경 + 흰 글리프) 옮겼습니다.
- 글리프 path·선 두께는 요청한 로고와 같게 유지했습니다.

## 검증 결과 (실제 Chrome)

| 페이지 | 아이콘 경로 | 응답 |
|---|---|---|
| `/` | `/images/favicon.svg` | 200 `image/svg+xml` |
| `/html/loadview/load-view.html` | `/images/favicon.svg` | 200 `image/svg+xml` |
| `/html/fabric/fabric-editor.html` | `/images/favicon.svg` | 200 `image/svg+xml` |

16·32·64px로 밝은/어두운 탭 배경 위에 그려 확인했고, 16px에서도 로고 모양이 구분됩니다.

## 커밋·푸시

사용자 요청으로 두 저장소를 커밋하고 `origin/main`에 푸시했습니다.

| 저장소 | 커밋 |
|---|---|
| `sj-lab-mapservice` | v1.28~v1.29 시설물 선택 가운데 이동·팝업 표시 시점 / v1.30 탭 아이콘 |
| `mapservice-rest` | history v1.28~v1.30 + 공유 페이지 |
