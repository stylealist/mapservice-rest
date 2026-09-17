package com.example.mapservice.service;

/**
 * 지도 WFS 레이어 GeoJSON 조회.
 *
 * bbox 가 null 이면 뷰에 이미 조립된 전국 FeatureCollection 을 그대로 내려주고(기존 동작),
 * bbox 가 있으면 그 화면 영역 안의 피처만 limit 개까지 조립해 내려준다.
 * bbox 는 {minX, minY, maxX, maxY} 4개 원소이며 좌표계는 EPSG:3857 이다.
 */
public interface WfsService {
    String convenienceStore(double[] bbox, int limit) throws Exception;
    String busStopInfo(double[] bbox, int limit) throws Exception;
    String cctvInfo(double[] bbox, int limit) throws Exception;
    String pharmacyInfo(double[] bbox, int limit) throws Exception;
    String hospitalInfo(double[] bbox, int limit) throws Exception;
    String governmentOfficeInfo(double[] bbox, int limit) throws Exception;
}
