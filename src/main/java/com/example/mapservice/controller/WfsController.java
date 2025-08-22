package com.example.mapservice.controller;

import com.example.mapservice.service.WfsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class WfsController {
    private static WfsService wfsService;
    public WfsController(WfsService wfsService){this.wfsService = wfsService;}

    @GetMapping("/convenience-store")
    public ResponseEntity<String> convenienceStore() throws Exception {
        String geojson = "";
        try{
            geojson = wfsService.convenienceStore();
             // 바디는 JSON 문자열 그대로
        }catch (Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .body(geojson);
    }
}
