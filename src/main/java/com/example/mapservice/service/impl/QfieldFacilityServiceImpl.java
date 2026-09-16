package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.QfieldFacilityMapper;
import com.example.mapservice.service.QfieldFacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class QfieldFacilityServiceImpl implements QfieldFacilityService {

    private final QfieldFacilityMapper mapper;

    @Override
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
        return mapper.getFacilities(code);
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
     * 시설물 아이콘 설정(qfield.facility_icon) 조회.
     * 테이블이 아직 없거나 조회가 실패해도 지도 전체가 멈추지 않도록 null 을 반환하고,
     * 프론트엔드는 내장 기본 아이콘으로 표시한다(db/qfield_facility_icon.sql 참고).
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
