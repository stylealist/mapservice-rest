# history v1.23 — 운영 워커가 음성 변환 텍스트를 빈 값으로 덮어쓰던 문제 수정

- **날짜**: 2026-09-17
- **영향 저장소**: `sj-qfieldsync`(동기화 배치)
- **이전 버전**: [history_v1.22.md](history_v1.22.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 팝업 "음성 변환 내용" 확인 |

## 실행한 프롬프트

```
음성내용변환 부분에 값을 audio_memo_txt 컬럼 값으로 나오도록 해줘
db에 값은 들어가있는데 안나와서 물어보는거야 나오도록 해줘
아까 qfieldsync 로컬에서 실행할때는 텍스트가 저장된걸 확인했는데 실서버를 구동했더니 다시 빈값으로 나오고있어
```

## 확인한 사실

- 화면은 이미 `audio_memo_txt`를 참조하고 있었고, 값을 넣어 재현하면 정상 표시됩니다.
- 조회 시점의 DB에는 값이 없었습니다(모든 `_txt` 컬럼 0건). **로컬 워커가 채운 값을 운영 워커가 덮어쓰고 있었기 때문**입니다.
- 두 워커가 **같은 DB를 대상으로 동시에 동작**하며, UPSERT가 `_txt` 컬럼까지 무조건 덮어씁니다. 운영에서 STT가 실패하면 빈 문자열이 그대로 덮어씁니다.

> 참고: MCP 조회 계정과 백엔드 datasource는 **서로 다른 DB**였습니다. MCP 쪽에는 `qfield.facility_icon`이 없어 확인되었고, 이후 조사는 백엔드 datasource로 직접 했습니다.

## 변경된 결과물

| 파일 | 변경 |
|---|---|
| `qfield_data_sync.py` | ① `_txt` 컬럼은 빈 값으로 덮어쓰지 않도록 UPSERT 수정 ② `APP_ENV` 인식 및 다운로드 경로 환경변수화 ③ 기동 시 STT 환경 점검 로그 |

### 1. 빈 값 덮어쓰기 방지 (핵심)

```sql
-- 이전: 항상 덮어씀
"audio_memo_txt" = EXCLUDED."audio_memo_txt"

-- 이후: 이번 결과가 비면 기존 값 유지
"audio_memo_txt" = COALESCE(NULLIF(EXCLUDED."audio_memo_txt", ''), qfield."<table>"."audio_memo_txt")
```

STT가 실패해도 **이미 저장된 텍스트는 보존**되고, 성공하면 새 값으로 갱신됩니다.

### 2. 환경변수 불일치

배포 차트는 `APP_ENV=prod`를 주는데 코드는 `FLASK_ENV`만 봐서, **운영에서도 로컬 경로(`D:/work/qfield`)를 쓰고 있었습니다.** 리눅스에서는 그 이름의 상대 디렉터리가 생성되어 동작은 했지만, 차트가 붙인 볼륨(`/app/qfield`)이 아니라 컨테이너 레이어에 파일이 쌓이고 있었습니다.

- `APP_ENV`와 `FLASK_ENV`를 모두 인정
- 운영 기본 경로를 차트 볼륨과 같은 `/app/qfield`로 정렬
- `QFIELD_DOWNLOAD_DIR`로 덮어쓰기 가능

### 3. 기동 점검 로그

파드 로그만 보고 원인을 좁힐 수 있도록 기동 시 한 번 찍습니다.

```
⚙️ ENV=prod / 다운로드 경로=/app/qfield
⚙️ STT: 모듈 로드됨 / ffmpeg=있음 (/usr/bin/ffmpeg)
⚙️ STT: 외부 네트워크 연결 확인      ← 실패하면 "구글 STT 호출 불가"
```

## 검증 결과

| 확인 | 결과 |
|---|---|
| UPSERT 동작 | 임시 테이블로 검증 — 빈 값 재동기화 후에도 기존 텍스트 **유지**, 다른 컬럼은 정상 갱신, STT 성공 시 새 값으로 갱신 (검증 후 롤백, 운영 데이터 변경 없음) |
| 문법 | `py_compile` 통과 |
| 환경 점검 로직 | 로컬 실행 시 모듈 로드·ffmpeg 경로·외부 네트워크 모두 정상 출력 |

## 남은 원인 추적

ffmpeg는 최초 배포(2026-08-27)부터 이미지에 포함되어 있고 배포 태그 7은 2026-09-03 빌드라, **ffmpeg 누락은 가능성이 낮습니다.** 가장 유력한 후보는 **컨테이너의 외부 네트워크 차단**(구글 STT 호출 불가)입니다. 배포 후 기동 로그의 `⚙️ STT:` 세 줄이면 확정됩니다.

## 참고

- 운영 배포 전까지는 로컬 워커를 돌려 채운 값이 유지됩니다(이번 수정 적용 후).
- 로컬과 운영 워커를 **동시에 같은 DB로 돌리면** 서로 덮어쓸 수 있습니다. 검증 목적이라면 한쪽만 띄우세요.
