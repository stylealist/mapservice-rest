package com.example.mapservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface QfieldOfficeWorkMapper {
    String getFacilityRepairYn(@Param("totalId") String totalId);
    String getOfficeWorks(@Param("totalId") String totalId);
    String insertOfficeWork(@Param("totalId") String totalId, @Param("work") Map<String, Object> work);
    String updateOfficeWork(@Param("workId") long workId, @Param("work") Map<String, Object> work);
    int deleteOfficeWork(@Param("workId") long workId);
}
