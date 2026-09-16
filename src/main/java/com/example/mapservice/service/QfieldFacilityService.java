package com.example.mapservice.service;

public interface QfieldFacilityService {
    String getFacilities(String sidoCd, String sggCd, String emdCd);
    String getFacilityDetail(String totalId);
    String getSidoList();
    String getSggList(String sidoCd);
    String getEmdList(String sggCd);
    String getFacilityIcons();
}
