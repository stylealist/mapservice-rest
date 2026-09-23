-- ============================================================================
-- map.facility_reinspection — 시설물 현장 재점검 요청 기록
--
-- 목적: 내업(사무실 처리)을 완료해도 그것은 "사무실 처리가 끝났다"는 뜻이지
--       "현장 보수가 끝나 확인까지 됐다"는 뜻이 아니다. 보수 필요 여부
--       (qfield.facility_total_view.repair_required_yn)는 외업(현장조사 앱 → QFieldCloud →
--       sj-qfieldsync)이 채우는 값이라 이 서비스가 고칠 수 없고, 고쳐도 다음 동기화에
--       덮어써진다. 그래서 "처리는 끝났고 현장 확인만 남음" 상태를 map 스키마에 따로 남긴다.
--
--       흐름: 내업 완료(work_status='DONE') → 웹에서 "재점검 요청"(REQUESTED)
--             → 현장에서 확인되면 "재점검 확인"(CONFIRMED) 또는 요청 취소(CANCELED)
--       한 시설물에 열린 요청(REQUESTED)은 동시에 하나만 존재한다(아래 부분 UNIQUE 인덱스).
--
-- 외래키 주의: total_id 는 qfield.facility_total_view.total_id 를 가리키는 "논리적 FK"다
--   (뷰라서 물리 FK 불가 — 백엔드 QfieldReinspection* 계층이 등록 전에 존재를 검증한다).
--   work_id 도 map.facility_office_work 를 가리키지만 기록 삭제(소프트 삭제)와 독립적으로
--   남겨야 하므로 물리 FK 를 걸지 않는다.
--
-- 스키마 주의: 반드시 map 스키마에 둔다. qfield 스키마는 sj-qfieldsync 워커가 관리하며
--   QField 프로젝트 이름 패턴이 아닌 테이블을 "삭제된 프로젝트 테이블"로 보고 아카이브 후 DROP 한다.
--
-- 실행 주체: 에이전트는 실행하지 않는다(프로젝트 규칙: DB는 조회만).
--            DB 권한이 있는 담당자가 psql/DBeaver 등으로 실행할 것.
--   예) psql -h <호스트> -p <포트> -U <계정> -d sjlab -f db/map_facility_reinspection.sql
--
-- 이 테이블이 없어도 지도·시설물 목록·내업 기능은 그대로 동작한다.
--   - 재점검 목록 조회 API 는 빈 배열([])을 돌려준다(프론트는 배지를 표시하지 않음).
--   - 재점검 등록·변경 API 만 503(NOT_CONFIGURED)이 된다.
--
-- 재실행해도 안전하다(IF NOT EXISTS, 제약은 존재 확인 후 추가).
-- ============================================================================

CREATE TABLE IF NOT EXISTS map.facility_reinspection (
    reinspect_id bigserial     PRIMARY KEY,
    total_id     text          NOT NULL,                     -- 논리적 FK: qfield.facility_total_view.total_id
    work_id      bigint,                                     -- 논리적 FK: map.facility_office_work.work_id (어느 내업 건의 후속인지)
    status       varchar(20)   NOT NULL DEFAULT 'REQUESTED', -- REQUESTED / CONFIRMED / CANCELED
    reason       varchar(500),                               -- 재점검이 필요한 이유
    request_user varchar(100),                               -- 요청자(로그인 연동 전까지는 null)
    request_date timestamp     NOT NULL DEFAULT now(),
    confirm_user varchar(100),                               -- 확인자
    confirm_date timestamp,                                  -- 현장 확인 일시 (CONFIRMED 로 바뀐 시각)
    confirm_note varchar(500),                               -- 확인 결과 메모
    use_yn       char(1)       NOT NULL DEFAULT 'y',         -- 'n' 이면 삭제(소프트 삭제)
    update_at    timestamp     NOT NULL DEFAULT now()
);

-- 상태 값 제한 (CREATE TABLE IF NOT EXISTS 는 기존 테이블에 제약을 더하지 않으므로 따로 확인 후 추가)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_facility_reinspection_status'
          AND conrelid = 'map.facility_reinspection'::regclass
    ) THEN
        ALTER TABLE map.facility_reinspection
            ADD CONSTRAINT ck_facility_reinspection_status
            CHECK (status IN ('REQUESTED', 'CONFIRMED', 'CANCELED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_facility_reinspection_use_yn'
          AND conrelid = 'map.facility_reinspection'::regclass
    ) THEN
        ALTER TABLE map.facility_reinspection
            ADD CONSTRAINT ck_facility_reinspection_use_yn
            CHECK (use_yn IN ('y', 'n'));
    END IF;
END $$;

COMMENT ON TABLE  map.facility_reinspection              IS '시설물 현장 재점검 요청. 내업 완료 후 "현장 확인만 남음"을 표시한다(외업 값 repair_required_yn 은 건드리지 않음)';
COMMENT ON COLUMN map.facility_reinspection.reinspect_id IS '재점검 요청 ID';
COMMENT ON COLUMN map.facility_reinspection.total_id     IS '시설물 ID (qfield.facility_total_view.total_id, 논리적 FK)';
COMMENT ON COLUMN map.facility_reinspection.work_id      IS '근거가 된 내업 기록 ID (map.facility_office_work.work_id, 논리적 FK)';
COMMENT ON COLUMN map.facility_reinspection.status       IS '상태: REQUESTED(재점검 대기) / CONFIRMED(현장 확인 완료) / CANCELED(요청 취소)';
COMMENT ON COLUMN map.facility_reinspection.reason       IS '재점검이 필요한 이유';
COMMENT ON COLUMN map.facility_reinspection.request_user IS '요청자';
COMMENT ON COLUMN map.facility_reinspection.request_date IS '요청 일시';
COMMENT ON COLUMN map.facility_reinspection.confirm_user IS '확인자';
COMMENT ON COLUMN map.facility_reinspection.confirm_date IS '현장 확인 일시';
COMMENT ON COLUMN map.facility_reinspection.confirm_note IS '확인 결과 메모';
COMMENT ON COLUMN map.facility_reinspection.use_yn       IS '사용 여부(n 이면 삭제된 기록)';
COMMENT ON COLUMN map.facility_reinspection.update_at    IS '수정 일시';

-- 시설물별 최신 요청 조회용 (상세 화면, 목록 병합 조회)
CREATE INDEX IF NOT EXISTS ix_facility_reinspection_total_id
    ON map.facility_reinspection (total_id, reinspect_id DESC)
    WHERE use_yn = 'y';

-- 열린 요청(REQUESTED)은 시설물당 하나만 — 중복 요청을 DB 차원에서 막는다(백엔드는 409 로 응답).
CREATE UNIQUE INDEX IF NOT EXISTS uk_facility_reinspection_open
    ON map.facility_reinspection (total_id)
    WHERE status = 'REQUESTED' AND use_yn = 'y';

-- ----------------------------------------------------------------------------
-- 조회 전용 계정 권한 (필요 시)
-- ----------------------------------------------------------------------------
-- GRANT SELECT ON map.facility_reinspection TO mcp_readonly;
