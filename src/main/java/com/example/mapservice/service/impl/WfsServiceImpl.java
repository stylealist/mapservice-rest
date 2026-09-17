package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.WfsMapper;
import com.example.mapservice.service.WfsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WfsServiceImpl implements WfsService {
    private final WfsMapper mapper;

    @Override
    public String convenienceStore(double[] bbox, int limit) {
        return bbox == null ? mapper.convenienceStore()
                : mapper.convenienceStoreByBbox(bbox[0], bbox[1], bbox[2], bbox[3], limit);
    }

    @Override
    public String busStopInfo(double[] bbox, int limit) {
        return bbox == null ? mapper.busStopInfo()
                : mapper.busStopInfoByBbox(bbox[0], bbox[1], bbox[2], bbox[3], limit);
    }

    @Override
    public String cctvInfo(double[] bbox, int limit) {
        return bbox == null ? mapper.cctvInfo()
                : mapper.cctvInfoByBbox(bbox[0], bbox[1], bbox[2], bbox[3], limit);
    }

    @Override
    public String pharmacyInfo(double[] bbox, int limit) {
        return bbox == null ? mapper.pharmacyInfo()
                : mapper.pharmacyInfoByBbox(bbox[0], bbox[1], bbox[2], bbox[3], limit);
    }

    @Override
    public String hospitalInfo(double[] bbox, int limit) {
        return bbox == null ? mapper.hospitalInfo()
                : mapper.hospitalInfoByBbox(bbox[0], bbox[1], bbox[2], bbox[3], limit);
    }

    @Override
    public String governmentOfficeInfo(double[] bbox, int limit) {
        return bbox == null ? mapper.governmentOfficeInfo()
                : mapper.governmentOfficeInfoByBbox(bbox[0], bbox[1], bbox[2], bbox[3], limit);
    }
}
