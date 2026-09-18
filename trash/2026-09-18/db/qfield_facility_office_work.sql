-- ============================================================================
-- qfield.facility_office_work — 시설물 내업(사무실 처리) 기록 테이블
--
-- 목적: 외업(QField 앱)에서 보수 요청(repair_required_yn = 'Y')된 시설물을
--       내업에서 웹으로 확인하고, 처리 상태·담당·일정·비용 등 추가 정보를 기록한다.
--       한 시설물에 여러 건의 기록이 쌓이며, 가장 최근 기록(work_id 최대, use_yn = 'y')이
--       그 시설물의 현재 내업 상태가 된다.
--
-- 외래키 주의: total_id 는 qfield.facility_total_view.total_id 를 가리키는 "논리적 FK"다.
--   facility_total_view 는 sj-qfieldsync 가 재생성하는 뷰(VIEW)이므로 PostgreSQL 에서
--   물리적 FOREIGN KEY 제약을 걸 수 없다. 시설물 존재 여부는 백엔드(mapservice-rest,
--   QfieldOfficeWork* 계층)가 등록 전에 뷰를 조회해 검증한다.
--
-- 실행 이력: 2026-09-18 사용자 지시로 개발 DB(sjlab)에 실행.
-- 실행 주체: 기본적으로 에이전트는 실행하지 않는다(프로젝트 규칙: DB는 조회만).
--            DB 권한이 있는 담당자가 psql/DBeaver 등으로 실행하거나, 사용자가 명시적으로 지시할 것.
--   예) psql -h <호스트> -p <포트> -U <계정> -d sjlab -f db/qfield_facility_office_work.sql
--
-- 재실행해도 안전하다(IF NOT EXISTS, 제약은 존재 확인 후 추가).
-- 조회 계정(mcp_readonly 등)에 SELECT 권한이 필요하면 맨 아래 GRANT 문을 함께 실행한다.
-- ============================================================================

CREATE TABLE IF NOT EXISTS qfield.facility_office_work (
    work_id       bigserial     PRIMARY KEY,
    total_id      text          NOT NULL,                        -- 논리적 FK: qfield.facility_total_view.total_id
    work_status   varchar(20)   NOT NULL DEFAULT 'RECEIVED',     -- RECEIVED / IN_PROGRESS / DONE / HOLD
    work_content  text,                                          -- 처리 내용
    dept_nm       varchar(100),                                  -- 담당 부서
    manager_nm    varchar(50),                                   -- 담당자
    manager_tel   varchar(50),                                   -- 연락처
    plan_date     date,                                          -- 처리 예정일
    complete_date date,                                          -- 완료일
    cost          numeric(15,0),                                 -- 비용(원)
    vendor_nm     varchar(200),                                  -- 시공 업체
    contract_no   varchar(100),                                  -- 계약번호
    before_photo  text,                                          -- 처리 전 사진 경로
    after_photo   text,                                          -- 처리 후 사진 경로
    remark        text,                                          -- 비고
    use_yn        char(1)       NOT NULL DEFAULT 'y',            -- 'n' 이면 삭제(소프트 삭제)
    reg_date      timestamp     NOT NULL DEFAULT now(),
    update_at     timestamp     NOT NULL DEFAULT now()
);

-- 처리 상태 값 제한 (CREATE TABLE IF NOT EXISTS 는 기존 테이블에 제약을 더하지 않으므로 따로 확인 후 추가)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_facility_office_work_status'
          AND conrelid = 'qfield.facility_office_work'::regclass
    ) THEN
        ALTER TABLE qfield.facility_office_work
            ADD CONSTRAINT ck_facility_office_work_status
            CHECK (work_status IN ('RECEIVED', 'IN_PROGRESS', 'DONE', 'HOLD'));
    END IF;
END $$;

COMMENT ON TABLE  qfield.facility_office_work               IS '시설물 내업(사무실 처리) 기록. total_id 는 qfield.facility_total_view 의 논리적 FK(뷰라서 물리 FK 불가, 백엔드가 존재 검증)';
COMMENT ON COLUMN qfield.facility_office_work.work_id       IS '내업 기록 ID';
COMMENT ON COLUMN qfield.facility_office_work.total_id      IS '시설물 ID (qfield.facility_total_view.total_id, 논리적 FK)';
COMMENT ON COLUMN qfield.facility_office_work.work_status   IS '처리 상태: RECEIVED(접수) / IN_PROGRESS(처리중) / DONE(완료) / HOLD(보류)';
COMMENT ON COLUMN qfield.facility_office_work.work_content  IS '처리 내용';
COMMENT ON COLUMN qfield.facility_office_work.dept_nm       IS '담당 부서';
COMMENT ON COLUMN qfield.facility_office_work.manager_nm    IS '담당자';
COMMENT ON COLUMN qfield.facility_office_work.manager_tel   IS '담당자 연락처';
COMMENT ON COLUMN qfield.facility_office_work.plan_date     IS '처리 예정일';
COMMENT ON COLUMN qfield.facility_office_work.complete_date IS '처리 완료일';
COMMENT ON COLUMN qfield.facility_office_work.cost          IS '처리 비용(원)';
COMMENT ON COLUMN qfield.facility_office_work.vendor_nm     IS '시공 업체';
COMMENT ON COLUMN qfield.facility_office_work.contract_no   IS '계약번호';
COMMENT ON COLUMN qfield.facility_office_work.before_photo  IS '처리 전 사진 경로';
COMMENT ON COLUMN qfield.facility_office_work.after_photo   IS '처리 후 사진 경로';
COMMENT ON COLUMN qfield.facility_office_work.remark        IS '비고';
COMMENT ON COLUMN qfield.facility_office_work.use_yn        IS '사용 여부(n 이면 삭제된 기록)';
COMMENT ON COLUMN qfield.facility_office_work.reg_date      IS '등록 일시';
COMMENT ON COLUMN qfield.facility_office_work.update_at     IS '수정 일시';

-- 시설물별 최신 기록 조회용 (목록 API, 지도 목록의 최신 상태 LATERAL 조회)
-- 조회는 항상 use_yn = 'y' 조건이므로 부분 인덱스로 둔다.
CREATE INDEX IF NOT EXISTS ix_facility_office_work_total_id
    ON qfield.facility_office_work (total_id, work_id DESC)
    WHERE use_yn = 'y';

-- ----------------------------------------------------------------------------
-- 조회 전용 계정 권한 (필요 시)
-- ----------------------------------------------------------------------------
-- GRANT SELECT ON qfield.facility_office_work TO mcp_readonly;
