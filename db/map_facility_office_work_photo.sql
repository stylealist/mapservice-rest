-- ============================================================================
-- map.facility_office_work_photo — 시설물 내업 기록의 처리 전·후 사진(바이너리 저장)
--
-- 목적: 내업(사무실 처리) 기록에 처리 전(BEFORE)·처리 후(AFTER) 사진 파일을 첨부한다.
--       파일은 웹사이트 내업 작성 화면 → mapservice-rest 업로드 API 로만 쌓이며, DB 에 직접 넣지 않는다.
--       원본 바이너리를 content(bytea)에 그대로 저장한다(외부 파일 저장소 없음).
--       업로드 제한(백엔드가 검증): 처리 전·후 각 최대 5장, 장당 10MB, JPEG·PNG·WebP 만
--       (Content-Type 이 아니라 파일 시그니처로 실제 이미지인지 확인).
--
-- 외래키: work_id 는 같은 map 스키마의 실제 테이블 map.facility_office_work.work_id 를
--   가리키므로 물리적 FOREIGN KEY 를 건다. 내업 기록은 소프트 삭제(use_yn = 'n')라 행이 남으므로
--   ON DELETE CASCADE 는 쓰지 않는다(사진도 use_yn 으로 따로 소프트 삭제).
--   기존 facility_office_work.before_photo / after_photo 텍스트 컬럼은 기존 데이터 호환용으로 그대로 둔다.
--
-- 스키마 주의: 반드시 map 스키마에 둔다. qfield 스키마는 sj-qfieldsync 워커가 관리하며,
--   QField 프로젝트 이름 패턴이 아닌 테이블을 "삭제된 프로젝트 테이블"로 보고 아카이브 후 DROP 한다.
--   (2026-09-16 qfield.facility_icon, 2026-09-18 qfield.facility_office_work 가 이 동작으로 삭제됨)
--
-- 선행 조건: db/map_facility_office_work.sql 이 먼저 실행되어 있어야 한다(FK 대상).
-- 실행 이력: 2026-09-18 사용자 지시로 개발 DB(sjlab)의 map 스키마에 실행.
-- 실행 주체: 기본적으로 에이전트는 실행하지 않는다(프로젝트 규칙: DB는 조회만).
--            DB 권한이 있는 담당자가 psql/DBeaver 등으로 실행하거나, 사용자가 명시적으로 지시할 것.
--   예) psql -h <호스트> -p <포트> -U <계정> -d sjlab -f db/map_facility_office_work_photo.sql
--
-- 재실행해도 안전하다(IF NOT EXISTS, 제약은 존재 확인 후 추가).
-- ============================================================================

CREATE TABLE IF NOT EXISTS map.facility_office_work_photo (
    photo_id   bigserial     PRIMARY KEY,
    work_id    bigint        NOT NULL,                  -- FK: map.facility_office_work.work_id
    kind       varchar(10)   NOT NULL,                  -- BEFORE(처리 전) / AFTER(처리 후)
    file_name  varchar(255)  NOT NULL,                  -- 업로드한 원본 파일명
    mime_type  varchar(100)  NOT NULL,                  -- image/jpeg / image/png / image/webp
    file_size  integer       NOT NULL,                  -- 바이트
    content    bytea         NOT NULL,                  -- 이미지 원본 바이너리
    use_yn     char(1)       NOT NULL DEFAULT 'y',      -- 'n' 이면 삭제(소프트 삭제)
    reg_date   timestamp     NOT NULL DEFAULT now()
);

-- 제약 (CREATE TABLE IF NOT EXISTS 는 기존 테이블에 제약을 더하지 않으므로 따로 확인 후 추가)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_facility_office_work_photo_work_id'
          AND conrelid = 'map.facility_office_work_photo'::regclass
    ) THEN
        ALTER TABLE map.facility_office_work_photo
            ADD CONSTRAINT fk_facility_office_work_photo_work_id
            FOREIGN KEY (work_id) REFERENCES map.facility_office_work (work_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_facility_office_work_photo_kind'
          AND conrelid = 'map.facility_office_work_photo'::regclass
    ) THEN
        ALTER TABLE map.facility_office_work_photo
            ADD CONSTRAINT ck_facility_office_work_photo_kind
            CHECK (kind IN ('BEFORE', 'AFTER'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_facility_office_work_photo_mime_type'
          AND conrelid = 'map.facility_office_work_photo'::regclass
    ) THEN
        ALTER TABLE map.facility_office_work_photo
            ADD CONSTRAINT ck_facility_office_work_photo_mime_type
            CHECK (mime_type IN ('image/jpeg', 'image/png', 'image/webp'));
    END IF;
END $$;

COMMENT ON TABLE  map.facility_office_work_photo           IS '시설물 내업 기록의 처리 전·후 사진(바이너리). work_id 는 map.facility_office_work 물리 FK. map 스키마에 둘 것(qfield 스키마는 sj-qfieldsync 가 비프로젝트 테이블을 DROP)';
COMMENT ON COLUMN map.facility_office_work_photo.photo_id  IS '사진 ID';
COMMENT ON COLUMN map.facility_office_work_photo.work_id   IS '내업 기록 ID (map.facility_office_work.work_id)';
COMMENT ON COLUMN map.facility_office_work_photo.kind      IS '사진 구분: BEFORE(처리 전) / AFTER(처리 후). 구분별 최대 5장(백엔드 검증)';
COMMENT ON COLUMN map.facility_office_work_photo.file_name IS '업로드한 원본 파일명';
COMMENT ON COLUMN map.facility_office_work_photo.mime_type IS 'MIME 타입: image/jpeg / image/png / image/webp (파일 시그니처로 판별)';
COMMENT ON COLUMN map.facility_office_work_photo.file_size IS '파일 크기(바이트, 최대 10MB)';
COMMENT ON COLUMN map.facility_office_work_photo.content   IS '이미지 원본 바이너리';
COMMENT ON COLUMN map.facility_office_work_photo.use_yn    IS '사용 여부(n 이면 삭제된 사진)';
COMMENT ON COLUMN map.facility_office_work_photo.reg_date  IS '등록 일시';

-- 내업 기록별 사진 목록·장수 확인용. 조회는 항상 use_yn = 'y' 조건이므로 부분 인덱스로 둔다.
CREATE INDEX IF NOT EXISTS ix_facility_office_work_photo_work_id
    ON map.facility_office_work_photo (work_id, kind, photo_id)
    WHERE use_yn = 'y';

-- ----------------------------------------------------------------------------
-- 조회 전용 계정 권한 (필요 시)
-- ----------------------------------------------------------------------------
-- GRANT SELECT ON map.facility_office_work_photo TO mcp_readonly;
