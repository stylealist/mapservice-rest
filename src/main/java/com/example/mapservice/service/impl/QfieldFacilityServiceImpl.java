package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.QfieldFacilityMapper;
import com.example.mapservice.service.QfieldFacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class QfieldFacilityServiceImpl implements QfieldFacilityService {

    // 내업 테이블이 없다고 판단한 뒤 다시 확인하기까지의 간격.
    // 배포 뒤 담당자가 db/map_facility_office_work.sql 을 실행하면 재기동 없이 이 시간 안에 반영된다.
    private static final long officeWorkRecheckMillis = 60_000L;

    private final QfieldFacilityMapper mapper;

    // 내업 테이블(map.facility_office_work) 존재 여부 캐시. 요청마다 to_regclass 를 부르지 않기 위함.
    // 한 번 있다고 확인되면 계속 믿고, 조회가 실패하면(테이블 삭제·이름 변경) 그때 없음으로 바꾼다.
    private volatile boolean officeWorkTableExists;
    private volatile long officeWorkCheckedAt;

    /**
     * 시설물 목록. 내업 테이블이 있으면 최신 내업 상태를 붙이고, 없으면 기록 없음으로 채운 폴백 쿼리를 쓴다.
     * 내업 테이블이 없는 DB(스크립트 미실행 환경)에서도 지도 핵심 기능인 목록이 500 이 되지 않게 하기 위함.
     * 첫 쿼리가 실패해도 폴백 쿼리가 같은 트랜잭션에 묶여 abort 되지 않도록 트랜잭션 없이 실행한다.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getFacilities(String sidoCd, String sggCd, String emdCd) {
        // 가장 구체적인 값 하나만 적용 (emdCd > sggCd > sidoCd)
        String code = null;
        if (emdCd != null && !emdCd.isBlank()) {
            code = emdCd.trim();
        } else if (sggCd != null && !sggCd.isBlank()) {
            code = sggCd.trim();
        } else if (sidoCd != null && !sidoCd.isBlank()) {
            code = sidoCd.trim();
        }

        if (isOfficeWorkTableAvailable()) {
            try {
                return mapper.getFacilities(code);
            } catch (RuntimeException e) {
                if (!isUndefinedTable(e)) {
                    throw e;
                }
                // 있다고 캐시한 뒤 테이블이 사라진 경우 — 폴백으로 이어서 응답한다
                updateOfficeWorkTableState(false);
            }
        }
        return mapper.getFacilitiesWithoutOfficeWork(code);
    }

    private boolean isOfficeWorkTableAvailable() {
        if (officeWorkTableExists) {
            return true;
        }
        long checkedAt = officeWorkCheckedAt;
        if (checkedAt != 0 && System.currentTimeMillis() - checkedAt < officeWorkRecheckMillis) {
            return false;
        }
        boolean exists;
        try {
            exists = mapper.existsOfficeWorkTable();
        } catch (RuntimeException e) {
            exists = false;
        }
        updateOfficeWorkTableState(exists);
        return exists;
    }

    // 상태가 바뀔 때만 한 줄 남긴다(매 요청·매 재확인마다 찍지 않음)
    private synchronized void updateOfficeWorkTableState(boolean exists) {
        boolean firstCheck = officeWorkCheckedAt == 0;
        if (exists && !officeWorkTableExists && !firstCheck) {
            log.info("내업 테이블(map.facility_office_work)이 확인되어 시설물 목록에 내업 상태를 포함합니다.");
        } else if (!exists && (officeWorkTableExists || firstCheck)) {
            log.warn("내업 테이블(map.facility_office_work)이 없어 시설물 목록의 내업 상태를 기록 없음(보수 필요=PENDING, 그 외 null)으로 내려줍니다. "
                    + "db/map_facility_office_work.sql 을 실행하세요.");
        }
        officeWorkTableExists = exists;
        officeWorkCheckedAt = System.currentTimeMillis();
    }

    // PostgreSQL undefined_table(42P01) 인지 원인 체인에서 확인
    private boolean isUndefinedTable(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && "42P01".equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getFacilityDetail(String totalId) {
        return mapper.getFacilityDetail(totalId);
    }

    @Override
    public String getSidoList() {
        return mapper.getSidoList();
    }

    @Override
    public String getSggList(String sidoCd) {
        return mapper.getSggList(sidoCd);
    }

    @Override
    public String getEmdList(String sggCd) {
        return mapper.getEmdList(sggCd);
    }

    /**
     * 시설물 아이콘 설정(map.facility_icon) 조회.
     * 테이블이 아직 없거나 조회가 실패해도 지도 전체가 멈추지 않도록 null 을 반환하고,
     * 프론트엔드는 내장 기본 아이콘으로 표시한다(db/map_facility_icon.sql 참고).
     */
    @Override
    public String getFacilityIcons() {
        try {
            return mapper.getFacilityIcons();
        } catch (Exception e) {
            log.warn("시설물 아이콘 설정 조회 실패 — 프론트엔드 기본 아이콘으로 표시됩니다. 원인: {}", e.getMessage());
            return null;
        }
    }
}
