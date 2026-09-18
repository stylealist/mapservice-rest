package com.example.mapservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QfieldFacilityMapper {
    String getFacilities(@Param("code") String code);
    String getFacilitiesWithoutOfficeWork(@Param("code") String code);
    boolean existsOfficeWorkTable();
    String getFacilityDetail(@Param("totalId") String totalId);
    String getSidoList();
    String getSggList(@Param("sidoCd") String sidoCd);
    String getEmdList(@Param("sggCd") String sggCd);
    String getFacilityIcons();
    String getFacilityMediaSource(@Param("totalId") String totalId);
}
