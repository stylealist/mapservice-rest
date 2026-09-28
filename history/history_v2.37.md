# history v2.37 — 로컬에서 동영상 재생 위치를 옮길 수 없던 문제 수정(정적 서버 Range 지원)

- **날짜**: 2026-09-28
- **영향 저장소**: `mapservice-rest`(로컬 스크립트·문서), `sj-lab-mapservice`(README)
- **이전 버전**: [history_v2.36.md](history_v2.36.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 로컬 지도 | http://localhost:4000 | 이제 Node 정적 서버가 서빙 |
| 로컬 영상 | http://localhost:4000/videos/intro-summary.mp4 | `Range` 요청 시 206 응답 |
| 운영 지도 | https://sj-lab.co.kr/map/ | 배포 후 확인 |

## 실행한 프롬프트

```
지금도 동영상을 중간으로 넘길수가없어 예를들어 00:03초를 보고있는데 00:10초로 넘겨서 보려고하면 넘겨지지가않아
```

## 원인

영상 파일이나 화면 코드 문제가 아니라 **로컬 정적 서버**가 원인이었습니다.

```
$ curl -D - -o /dev/null -H "Range: bytes=100000-100100" http://localhost:4000/videos/intro-summary.mp4
HTTP/1.0 200 OK                      ← 206 이어야 함
Server: SimpleHTTP/0.6 Python/3.12.7
Content-Length: 4471363              ← 요청한 101바이트가 아니라 파일 전체
```

`python -m http.server`(`SimpleHTTPRequestHandler`)는 **`Range` 헤더를 무시하고 항상 200 + 전체 파일**을 돌려줍니다. 브라우저는 재생 위치를 옮길 때 그 지점의 바이트 범위를 요청하는데, 서버가 부분 응답을 못 하니 `Accept-Ranges`도 없고 진행 바를 끌어도 되돌아왔습니다.

영상 자체는 정상이었습니다 — 두 파일 모두 atom 순서가 `ftyp → moov → free → mdat`로 **moov가 앞에 있어(faststart)** 전체를 받지 않아도 탐색할 수 있는 구조입니다.

## 작업 내용

| 파일 | 변경 |
|---|---|
| `scripts/static-server.js` | **신규**. Node 기본 모듈만 쓰는 정적 서버. `Range: bytes=...`를 해석해 **206 Partial Content**로 응답하고(`Content-Range`, 끝 범위 생략·마지막 N바이트 형식 포함, 잘못된 범위는 416), 모든 응답에 `Accept-Ranges: bytes`를 붙입니다. 문서 루트 밖 경로 차단, 확장자별 MIME(mp4·m4a·webp·geojson 등), 디렉터리 요청 시 `index.html`, 로컬이므로 `Cache-Control: no-cache` |
| `scripts/local-stack.ps1` | 프론트 정적 서버를 `python -m http.server` → **`node scripts/static-server.js`** 로 교체. `node`가 없으면 경고 한 줄을 찍고 예전 방식으로 폴백(그 경우 seek 불가) |
| `docs/dev-environment.md` | 포트 표(4000)와 주의사항에 Range 이야기 추가 — python 서버로 띄우지 말 것 |
| `sj-lab-mapservice/README.md` | "6. 로컬 실행 및 확인"을 통합 스택 스크립트 / Node 정적 서버 기준으로 교체하고, python 서버의 한계를 명시 |

## 검증 (로컬)

서버 교체 전후로 같은 요청을 비교하고, 헤드리스 Chrome에서 실제로 위치를 옮겨 봤습니다.

```
$ curl -D - -o /dev/null -H "Range: bytes=100000-100100" http://localhost:4000/videos/intro-summary.mp4
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-Range: bytes 100000-100100/4471363
Content-Length: 101
```

| 확인 | 결과 |
|---|---|
| 요약본 초기 | `intro-summary.mp4`, 길이 65.9초, seekable 0~65.9 |
| 40초로 이동 | 현재 42.5초, 재생 계속 |
| 10초로 되감기 | 현재 12.0초, 재생 계속 |
| 전체본 전환 | `full-demo.mp4`, 길이 370.3초, seekable 0~370.3 |
| 200초로 이동 | 현재 203.4초, 재생 계속 |

포트 4000은 이제 `node`(pid 14160)가 잡고 있습니다.

## 참고

- 운영은 nginx가 정적 파일을 서빙하며 Range를 기본 지원하므로 같은 증상이 없을 것으로 보지만, **영상 파일이 아직 운영에 배포되지 않았으므로 배포 후 실제 브라우저에서 한 번 확인**해야 합니다.
- 이전 버전의 history 문서(v1.0 등)에 적힌 `python -m http.server 4100 --directory history/web`도 같은 한계가 있지만, 그 페이지에는 동영상이 없어 그대로 둡니다.

## 남은 작업

- 커밋·push 여부 확인(v2.36 변경분과 함께).
- 운영 배포 후 지도 사이트에서 소개 영상 위치 이동 확인.
