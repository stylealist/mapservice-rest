package com.example.mapservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WfsMapper {
    String convenienceStore();
    String busStopInfo();
    String cctvInfo();
    String pharmacyInfo();
    String hospitalInfo();
    String governmentOfficeInfo();

    // 화면 영역(bbox) 안의 피처만 뽑아 상한(limit)까지 내려주는 조회.
    // 전국 데이터를 통째로 만들지 않으므로 응답 크기와 DB 시간이 크게 줄어든다.
    // bbox 좌표계는 각 원본 테이블의 geom 과 같아야 한다(편의점만 SRID 0, 나머지는 3857).
    String convenienceStoreByBbox(@Param("minX") double minX, @Param("minY") double minY,
                                  @Param("maxX") double maxX, @Param("maxY") double maxY,
                                  @Param("limit") int limit);

    String busStopInfoByBbox(@Param("minX") double minX, @Param("minY") double minY,
                             @Param("maxX") double maxX, @Param("maxY") double maxY,
                             @Param("limit") int limit);

    String cctvInfoByBbox(@Param("minX") double minX, @Param("minY") double minY,
                          @Param("maxX") double maxX, @Param("maxY") double maxY,
                          @Param("limit") int limit);

    String pharmacyInfoByBbox(@Param("minX") double minX, @Param("minY") double minY,
                              @Param("maxX") double maxX, @Param("maxY") double maxY,
                              @Param("limit") int limit);

    String hospitalInfoByBbox(@Param("minX") double minX, @Param("minY") double minY,
                              @Param("maxX") double maxX, @Param("maxY") double maxY,
                              @Param("limit") int limit);

    String governmentOfficeInfoByBbox(@Param("minX") double minX, @Param("minY") double minY,
                                      @Param("maxX") double maxX, @Param("maxY") double maxY,
                                      @Param("limit") int limit);
}
