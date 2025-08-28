package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.WfsMapper;
import com.example.mapservice.service.WfsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WfsServiceImpl implements WfsService {
    private final WfsMapper mapper;
    @Override
    public String convenienceStore() {
        return mapper.convenienceStore();
    }
    public String busStopInfo() {return mapper.busStopInfo();}
}
