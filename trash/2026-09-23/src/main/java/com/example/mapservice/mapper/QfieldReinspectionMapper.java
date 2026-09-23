package com.example.mapservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface QfieldReinspectionMapper {
    /** 재점검 테이블(map.facility_reinspection) 존재 여부. 없으면 false */
    boolean existsReinspectionTable();

    /** 시설물 존재 및 보수 필요 여부('Y'/'N'). 시설물이 없으면 null */
    String getFacilityRepairYn(@Param("totalId") String totalId);

    /** 행정구역 범위의 열린 요청(REQUESTED) 목록 — 목록 화면에서 배지 표시용 */
    String getOpenReinspections(@Param("code") String code);

    /** 시설물 한 건의 재점검 요청 이력 */
    String getReinspections(@Param("totalId") String totalId);

    /** 열린 요청(REQUESTED)이 이미 있으면 그 ID, 없으면 null */
    Long findOpenReinspectId(@Param("totalId") String totalId);

    String insertReinspection(@Param("totalId") String totalId, @Param("item") Map<String, Object> item);

    String updateReinspection(@Param("reinspectId") long reinspectId, @Param("item") Map<String, Object> item);

    int deleteReinspection(@Param("reinspectId") long reinspectId);
}
