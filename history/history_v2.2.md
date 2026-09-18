# history v2.2 — 시설물 내업 사진 첨부 API 및 멀티 저장소 에이전트 가이드 정비

- **날짜**: 2026-09-18
- **영향 저장소**: `mapservice-rest`(DB 스크립트·백엔드 사진 API), `sj-lab-apigateway`(설정 정비), 연결된 전체 10개 저장소(`AGENTS.md`, `.agents/`)
- **이전 버전**: [history_v2.1.md](history_v2.1.md)
- **진행 방식**: Orca 오케스트레이션 / Antigravity 연계 작업

## 접속 URL

| 대상 | URL | 비고 |
|---|---|---|
| 공유 웹사이트(발행, 링크 공유 가능) | https://claude.ai/artifact/HhEYu2UmxSko5h8uef7hB9 | 버전 전환기로 v1.0~ 전체 열람 |
| 공유 웹사이트(로컬 파일) | [file:///C:/developer/workspace/mapservice-rest/history/web/index.html](file:///C:/developer/workspace/mapservice-rest/history/web/index.html) | 오프라인 요약 페이지 |
| 사진 업로드/조회 API | http://localhost:8100/map/qfield/facilities/{totalId}/office-works/{workId}/photos | 게이트웨이 경유 |
| 프론트엔드 | http://localhost:4000 | 내업 작성 및 상세 확인 |

## 실행한 프롬프트

```
/orchestration 지금 연결된 모든 프로젝트에 있는내용들 commit push 진행해줘
```

## 변경된 결과물

### 1. `mapservice-rest` (백엔드)
- **DB**: `db/map_facility_office_work_photo.sql` 신설 (`map.facility_office_work_photo` 바이너리 bytea 저장, 물리 FK `work_id`, 소프트 삭제 `use_yn`)
- **API**:
  - `POST /map/qfield/facilities/{totalId}/office-works/{workId}/photos`: 사진 업로드 (multipart, 구분별 최대 5장, 장당 10MB, 파일 매직넘버 검증)
  - `GET /map/qfield/facilities/{totalId}/office-works/{workId}/photos`: 사진 메타데이터 목록 조회
  - `GET /map/qfield/office-works/photos/{photoId}`: 이미지 바이너리 스트리밍 (Cache-Control 1시간)
  - `DELETE /map/qfield/office-works/photos/{photoId}`: 소프트 삭제 (204)
- **예외 및 설정**:
  - `CustomizedResponseEntityExceptionHandler`: 10MB 초과 시 413 JSON 에러 핸들러
  - `application.yml`: tomcat `max-swallow-size: 20MB`, multipart 한도 설정
  - `CLAUDE.md`, `docs/system-architecture.md`: 사진 첨부 API 계약 및 스키마 규칙 갱신

### 2. `sj-lab-apigateway` (게이트웨이)
- `Dockerfile`: Eclipse Temurin 17 베이스 이미지 지정 및 주석 오타 수정
- `src/main/resources/application.yml`: loadbalancer 설정 중복 축약 및 CORS 중복 헤더 제거(`DedupeResponseHeader`) 필터 위치 정상화

### 3. 전체 10개 프로젝트 공통
- `AGENTS.md` 및 `.agents/`: AI 에이전트(Antigravity, Orca 등) 협업 가이드 및 공용 스킬(`orca-cli`, `orchestration`) 설정 추가

## 검증 결과
- `mapservice-rest`: Maven 빌드 및 컴파일 통과 (`mvnw.cmd compile` SUCCESS)
- 전체 연결된 10개 저장소 git 상태 점검 및 커밋/푸시 준비 완료
