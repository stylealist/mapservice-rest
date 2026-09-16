# history v1.22 — 음성 변환(STT)이 DB에 들어오지 않던 원인 정리와 코드 보강

- **날짜**: 2026-09-16
- **영향 저장소**: `sj-qfieldsync`(동기화 배치), `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.21.md](history_v1.21.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 지도 서비스(로컬) | http://localhost:4000 | 팝업 항목 확인 |
| QFieldCloud | https://qfield.sj-lab.co.kr | 음성 파일 원본 |

## 실행한 프롬프트

```
/orchestration 음성 메모 텍스트는 상세팝업에서 제거해주고 sj-qfieldsync에서 음성파일을 받아서
텍스트로변환하는 부분이 있는데 해당 부분이 잘 이루어지지않아 db에 음성 텍스트가 안들어와있어
```

## 조사 결과

### 1. 팝업이 엉뚱한 필드를 보고 있었다

앱 소스(`projectutils.cpp`)를 확인하니 `facility_memo`는 **텍스트 입력(TextEdit) 필드**이고, 음성은 `audio_memo`(ExternalResource, DocumentViewer=3 Audio)였습니다.

그런데 동기화 워커는 컬럼명에 `record`/`audio`/`memo`가 들어가면 무조건 `<컬럼>_txt`를 만들어 STT를 시도했습니다. 그래서 **`facility_memo_txt`는 구조적으로 항상 빈 값**이었고, 팝업은 그 필드를 "음성 변환 내용"으로 보여주고 있었습니다. 실제 변환 결과가 담기는 필드는 **`audio_memo_txt`** 입니다.

### 2. 변환 자체는 로컬에서 정상 동작

실제 음성 파일(`AUDIO_20260916072658700.m4a`, 158KB)을 내려받아 `disaster2convert.read_audio()`를 직접 실행한 결과, 한국어 인식에 성공했습니다(신뢰도 0.87).

> 결과: "태풍이 와서 시설물이 파손되었습니다 시설물의 위치는 건물 2층에 있습니다"

즉 코드·라이브러리·구글 STT 호출 자체는 문제가 없습니다. **운영 컨테이너에서만 실패**하고 있습니다.

### 3. 실패 원인이 로그에 남지 않았다

DB 값은 `NULL`이 아니라 **빈 문자열**이었습니다. 즉 코드가 돌았고 결과가 비어 있었다는 뜻인데, 원인을 알 수 없었습니다.

```python
try:
    stt_val = dc.read_audio(path)
except Exception:
    pass          # ← 모든 오류를 조용히 삼킴
```

모듈 로드 실패, 파일 없음, ffmpeg 없음, 외부 네트워크 차단 중 무엇인지 구분할 방법이 없었습니다.

## 변경된 결과물

| 저장소 | 파일 | 변경 |
|---|---|---|
| `sj-qfieldsync` | `qfield_data_sync.py` | STT 실패를 원인별로 로그에 남김, `_txt` 생성 대상을 실제 오디오 컬럼으로 한정, 모듈 로드 실패 메시지에 예외 내용 포함 |
| `sj-qfieldsync` | `disaster2convert.py` | 임시 wav를 임시 디렉터리에 만들고 반드시 정리, 예외를 호출부로 전달, 인식 실패(`UnknownValueError`)만 빈 문자열 처리 |
| `sj-lab-mapservice` | `js/modules/map/map-facility.js` | 팝업의 "음성 변환 내용"을 `audio_memo_txt`로 교체하고, 항상 비는 `facility_memo_txt`는 숨김 |

### 함께 고쳐진 잠재 버그

- 기존 `read_audio`는 입력이 이미 `.wav`인 경우 **원본 파일을 삭제**했습니다(변환본이 아니라 원본을 지움).
- 변환에 실패하면 존재하지 않는 파일을 `os.remove` 하여 **2차 예외**가 났습니다.
- 임시 wav를 프로젝트 폴더에 만들어 다음 스캔에 섞일 수 있었습니다 → 임시 디렉터리로 분리.

## 검증 결과

로컬에서 함수 단위로 확인했습니다(운영 DB에는 쓰지 않음).

| 확인 | 결과 |
|---|---|
| 실제 m4a 변환 | 한국어 40자 인식 성공, **원본 파일 보존**, 임시 wav 잔여 0개 |
| 깨진 파일 | 예외가 호출부로 전달됨(`CouldntDecodeError`) → 이제 로그에 남음 |
| `_txt` 생성 판정 | `audio_memo` → 생성, `facility_memo`·`video`·`fclt_nm` → 생성 안 함 |
| 팝업 | 항목 19개, "음성 변환 내용"이 `audio_memo_txt` 기준으로 표시, 콘솔 오류 0건 |

## 남은 작업

- **운영 원인 확정은 파드 로그가 필요합니다.** 로컬에 쿠버네티스 접근 권한이 없어 확인하지 못했습니다. 수정본을 배포한 뒤 `sj-qfieldsync` 파드 로그에서 아래 중 어떤 줄이 찍히는지 보면 바로 좁혀집니다.
  - `⚠️ STT 모듈 로드 실패(...)` → 컨테이너에 의존 라이브러리 없음
  - `⚠️ STT 건너뜀(파일 없음)` → 다운로드 폴더에 음성 파일이 없음
  - `❌ STT 실패(CouldntDecodeError)` → ffmpeg 문제
  - `❌ STT 실패(RequestError)` → 구글 STT API 접근 불가(외부 네트워크)
  - `⚠️ STT 결과 없음` → 무음이거나 인식 실패
- 기존 테이블에 이미 만들어진 `facility_memo_txt` 컬럼은 남아 있지만 더 이상 채워지지 않습니다.
