# history v1.20 — 시설물 사진·음성·영상 실제 재생(QFieldCloud 중계)

- **날짜**: 2026-09-16
- **영향 저장소**: `mapservice-rest`(백엔드), `sj-lab-mapservice`(프론트엔드)
- **이전 버전**: [history_v1.19.md](history_v1.19.md)

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| QFieldCloud | https://qfield.sj-lab.co.kr | 첨부 파일 원본(인증 필요) |
| 미디어 중계 API | http://localhost:8100/map/qfield/facilities/{totalId}/media?path=... | 신규 |

## 실행한 프롬프트

```
사진, 오디오, 비디오가 실제로 재생이되었으면 좋겠어 qfieldcloud의 url은 https://qfield.sj-lab.co.kr/ 이야 해당 정보를 md 파일들에도 추가해줘
```

확인 질문에 대한 사용자 선택: **백엔드 프록시** 방식(파일을 공개하지 않고 백엔드가 중계)

## 왜 그냥 경로로는 재생이 안 되나

조사 결과 세 가지가 겹쳐 있었습니다.

1. DB 컬럼에는 URL이 아니라 **QField 프로젝트 내 상대 경로**가 저장됩니다 — `DCIM/JPEG_20260916071830596.jpg`, `audio/AUDIO_...m4a`, `video/VIDEO_...mp4`
2. 원본은 QFieldCloud에 있고 **API가 인증을 요구**합니다(`/api/v1/` → 401). 브라우저의 `<img>`·`<audio>`·`<video>`는 인증 헤더를 붙일 수 없습니다
3. `sj-qfieldsync`는 처리 후 **다운로드 폴더를 삭제**하고(`shutil.rmtree`) 차트 볼륨도 `emptyDir`라 서버에 파일이 남지 않습니다

## 변경된 결과물

| 저장소 | 파일 | 변경 |
|---|---|---|
| `mapservice-rest` | `service/QfieldMediaService.java` | 중계 인터페이스(결과·오류 유형 정의) |
| `mapservice-rest` | `service/impl/QfieldMediaServiceImpl.java` | QFieldCloud REST 호출, 토큰·프로젝트 캐시, 확장자 기반 Content-Type |
| `mapservice-rest` | `controller/QfieldFacilityController.java` | `GET /qfield/facilities/{totalId}/media?path=` 추가 |
| `mapservice-rest` | `mapper/QfieldFacilityMapper.java`, `mapper/qfield-facility.xml` | 시설물이 가진 첨부 경로·원본 프로젝트 조회 |
| `mapservice-rest` | `application.yml` | `qfield.base-url/username/password`(환경변수 주입) |
| `sj-lab-mapservice` | `js/modules/map/map-facility.js` | `buildFacilityMediaUrl()`로 중계 URL 조립, 갤러리·오디오·비디오에 적용 |
| 문서 | `CLAUDE.md`, `docs/system-architecture.md`, `docs/dev-environment.md`, 프론트 `docs/external-services.md` | QFieldCloud 주소·중계 방식·환경변수 규칙 |

### 중계 흐름

```
POST /api/v1/auth/login/     → 토큰 (6시간 캐시)
GET  /api/v1/projects/       → source_table 접두어(예: 41fc29aa-05da)로 프로젝트 식별 (캐시)
GET  /api/v1/files/{projectId}/{경로}/  → 파일 바이트
```

### 안전장치

- **경로 검증**: 요청된 경로가 그 시설물의 첨부(`photo_1`~`photo_5`, `audio_memo`, `video`)인지 DB로 확인한 뒤에만 전달합니다. 아니면 **403**. 임의 파일 접근을 막는 핵심이라 빼면 안 됩니다.
- **Content-Type 교정**: QFieldCloud는 `application.force-download`를 돌려줘 브라우저가 재생하지 못합니다. 확장자로 실제 타입(`image/jpeg`, `audio/mp4`, `video/mp4` 등)을 정해 내려줍니다.
- **토큰 만료 대응**: 401/403이면 토큰을 버리고 한 번만 재발급해 재시도합니다.
- **계정 미설정 시**: 이 엔드포인트만 **503**이고 나머지 기능은 그대로 동작합니다.
- 계정은 `QFIELD_USERNAME`/`QFIELD_PASSWORD` 환경변수로만 주입하며 저장소 파일에 값을 남기지 않았습니다.

## 검증 결과

| 확인 | 결과 |
|---|---|
| 백엔드 직접 호출 — 사진 | 200, `image/jpeg`, 85,843 B |
| 백엔드 직접 호출 — 음성 | 200, `audio/mp4`, 158,027 B |
| 백엔드 직접 호출 — 영상 | 200, `video/mp4`, 1,147,822 B |
| 허용되지 않은 경로 | **403** |
| 게이트웨이 경유 | 3회 연속 200 |
| 브라우저 재생 | 이미지 1080×1920 로드 완료, 음성 `readyState 4`·9.7초, 영상 `readyState 4`·2.6초(1080폭), 갤러리 `1 / 5`, 영상은 206 부분 요청도 정상 |
| 콘솔 오류 | **0건** |

## 남은 작업

- **운영 배포 전 Secret 필요**: `sj-lab-k8s-manifests`의 `mapservice-rest` 차트에 `QFIELD_USERNAME`/`QFIELD_PASSWORD`를 Secret으로 주입해야 합니다. 아직 적용하지 않았습니다.
- 지도 프론트엔드는 실서버 `/map` 경로에서 서비스되지 않는 상태입니다(v1.10 확인).
- 참고: `sj-qfieldsync`의 DB·QFieldCloud 접속 정보가 저장소에 평문으로 있습니다(사용자 인지 완료, 별도 과제).
