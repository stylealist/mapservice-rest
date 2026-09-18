-- ============================================================================
-- map.facility_icon — 시설물 지도 아이콘 관리 테이블
--
-- 목적: 프론트엔드(js/modules/map/map-facility.js)에 하드코딩되어 있던
--       시설물 종류별 아이콘(판별 키워드·라벨·글리프·색상)을 DB에서 관리한다.
--       행을 추가/수정하면 배포 없이 지도 아이콘이 바뀐다.
--
-- 스키마 주의: 반드시 map 스키마에 둔다. qfield 스키마는 sj-qfieldsync 워커가 관리하며,
--   QField 프로젝트 이름 패턴이 아닌 테이블을 "삭제된 프로젝트 테이블"로 보고 아카이브 후 DROP 한다.
--   (2026-09-16 qfield.facility_icon 이 이 동작으로 삭제되어 2026-09-18 map 스키마로 이전함)
--
-- 실행 이력: 2026-09-16 qfield 스키마에 실행(history_v1.7.md) → 워커가 삭제.
--            2026-09-18 사용자 지시로 개발 DB(sjlab)의 map 스키마에 다시 실행.
-- 실행 주체: 기본적으로 에이전트는 실행하지 않는다(프로젝트 규칙: DB는 조회만).
--            DB 권한이 있는 담당자가 psql/DBeaver 등으로 실행하거나, 사용자가 명시적으로 지시할 것.
--   예) psql -h <호스트> -p <포트> -U <계정> -d sjlab -f db/map_facility_icon.sql
--
-- 조회 계정(mcp_readonly 등)에 SELECT 권한이 필요하면 맨 아래 GRANT 문을 함께 실행한다.
-- ============================================================================

CREATE TABLE IF NOT EXISTS map.facility_icon (
    id          serial       PRIMARY KEY,
    icon_type   varchar(50)  NOT NULL UNIQUE,                    -- 아이콘 식별자 (parking, charger ...)
    icon_label  varchar(100) NOT NULL,                           -- 팝업 배지에 표시할 종류 이름
    keywords    text[]       NOT NULL DEFAULT '{}',              -- fclt_nm 에 이 단어가 있으면 해당 아이콘 사용
    glyph_svg   text         NOT NULL,                           -- 핀 안에 그릴 SVG 조각. 문자열 COLOR 는 핀 색으로 치환됨
    pin_color   varchar(20)  NOT NULL DEFAULT '#2563eb',         -- 기본 핀 색
    warn_color  varchar(20)  NOT NULL DEFAULT '#d97706',         -- repair_required_yn = 'Y' 일 때 핀 색
    sort_order  integer      NOT NULL DEFAULT 0,                 -- 키워드 판별 우선순위 (작을수록 먼저)
    is_default  boolean      NOT NULL DEFAULT false,             -- 종류를 못 찾았을 때 쓰는 기본 아이콘 (한 건만 true)
    use_yn      char(1)      NOT NULL DEFAULT 'y',               -- 'n' 이면 사용하지 않음
    reg_date    timestamp    DEFAULT CURRENT_TIMESTAMP,
    update_at   timestamp    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  map.facility_icon            IS '시설물 지도 아이콘(종류별 글리프·색상·판별 키워드)';
COMMENT ON COLUMN map.facility_icon.icon_type  IS '아이콘 식별자 (parking, charger, hall, dining, sports, exhibition, default)';
COMMENT ON COLUMN map.facility_icon.keywords   IS 'fclt_nm 부분일치 판별 키워드 배열';
COMMENT ON COLUMN map.facility_icon.glyph_svg  IS '핀 안쪽 SVG 조각(24x32 viewBox 기준). 문자열 COLOR 가 핀 색으로 치환됨';
COMMENT ON COLUMN map.facility_icon.is_default IS '종류 미판별 시 사용하는 기본 아이콘 여부(한 건만 true)';

-- 기본 아이콘은 한 건만 존재하도록 보장
CREATE UNIQUE INDEX IF NOT EXISTS ux_facility_icon_default
    ON map.facility_icon ((is_default)) WHERE is_default;

-- ----------------------------------------------------------------------------
-- 초기 데이터 (프론트엔드에 하드코딩되어 있던 값과 동일)
-- 다시 실행해도 안전하도록 icon_type 기준 UPSERT
-- ----------------------------------------------------------------------------
INSERT INTO map.facility_icon
    (icon_type, icon_label, keywords, glyph_svg, sort_order, is_default)
VALUES
    ('parking', '주차장', ARRAY['주차'],
     '<text x="12" y="16.5" text-anchor="middle" font-family="Arial, sans-serif" font-size="12" font-weight="bold" fill="COLOR">P</text>',
     10, false),

    ('charger', '전기차 충전소', ARRAY['충전'],
     '<path d="M13.4 5.5 8 13.2h3.4L10.6 18.5 16 10.8h-3.4z" fill="COLOR"/>',
     20, false),

    ('hall', '강당·강의실', ARRAY['강당', '강의', '회의', '세미나', '교육', '대회의'],
     '<rect x="6" y="6.5" width="12" height="8" rx="1" fill="none" stroke="COLOR" stroke-width="1.6"/><path d="M12 14.5v2.8M9 17.3h6" stroke="COLOR" stroke-width="1.6" stroke-linecap="round"/>',
     30, false),

    ('dining', '구내식당·카페', ARRAY['식당', '카페', '급식', '매점'],
     '<path d="M8 6.5v4a2.6 2.6 0 0 0 2.6 2.6v4.4M10.6 6.5v4M13.2 6.5v4" fill="none" stroke="COLOR" stroke-width="1.6" stroke-linecap="round"/><path d="M16.4 6.5c1.2 0 1.8 1.4 1.8 3.2s-.6 3-1.8 3v4.8" fill="none" stroke="COLOR" stroke-width="1.6" stroke-linecap="round"/>',
     40, false),

    ('sports', '체육시설', ARRAY['테니스', '농구', '족구', '운동', '체육', '체력', '구장'],
     '<circle cx="12" cy="12" r="5.6" fill="none" stroke="COLOR" stroke-width="1.6"/><path d="M6.4 12h11.2M12 6.4v11.2" stroke="COLOR" stroke-width="1.3"/>',
     50, false),

    ('exhibition', '전시시설', ARRAY['전시', '홍보관', '박물'],
     '<rect x="6" y="7" width="12" height="10" rx="1" fill="none" stroke="COLOR" stroke-width="1.6"/><path d="M7.6 15l3-3.4 2.2 2.4 2-1.8 1.6 2.8z" fill="COLOR"/>',
     60, false),

    ('default', '시설물', ARRAY[]::text[],
     '<path d="M6.2 17.5V9.6L12 6l5.8 3.6v7.9z" fill="none" stroke="COLOR" stroke-width="1.6" stroke-linejoin="round"/><path d="M10.2 17.5v-3.4h3.6v3.4" fill="none" stroke="COLOR" stroke-width="1.4"/>',
     999, true)
ON CONFLICT (icon_type) DO UPDATE SET
    icon_label = EXCLUDED.icon_label,
    keywords   = EXCLUDED.keywords,
    glyph_svg  = EXCLUDED.glyph_svg,
    sort_order = EXCLUDED.sort_order,
    is_default = EXCLUDED.is_default,
    update_at  = CURRENT_TIMESTAMP;

-- 조회 전용 계정에 SELECT 권한 (계정명은 환경에 맞게 수정)
-- GRANT SELECT ON map.facility_icon TO mcp_readonly;

-- 확인용 조회
-- SELECT icon_type, icon_label, keywords, sort_order, is_default, use_yn FROM map.facility_icon ORDER BY sort_order;
