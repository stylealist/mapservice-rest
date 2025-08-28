package com.example.mapservice.service;

import org.springframework.http.ResponseEntity;


public interface WfsService {
    String convenienceStore() throws Exception;
    String busStopInfo() throws Exception;
}
