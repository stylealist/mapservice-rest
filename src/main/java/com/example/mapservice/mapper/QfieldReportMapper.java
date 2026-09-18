package com.example.mapservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface QfieldReportMapper {

    /**
     * 특정 시설물의 내업 상세 보고서 데이터 조회
     * workId 미지정 시 가장 최신 내업 기록을 조회합니다.
     */
    Map<String, Object> getFacilityReportData(@Param("totalId") String totalId,
                                              @Param("workId") Long workId);

    /**
     * 해당 내업 기록(workId)에 등록된 처리 전·후 사진 목록 (바이너리 content 포함)
     */
    List<Map<String, Object>> getPhotosWithContent(@Param("workId") long workId);

    /**
     * 조건별 시설물 내업 현황 목록 보고서 데이터 조회 (완료/보류 시설물 대상)
     */
    List<Map<String, Object>> getFacilityReportList(@Param("statuses") List<String> statuses,
                                                    @Param("code") String code,
                                                    @Param("keyword") String keyword);
}
