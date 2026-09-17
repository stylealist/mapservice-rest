package com.example.mapservice.controller;

import com.example.mapservice.service.WfsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WfsController {

    /** bbox 를 줬는데 limit 를 안 준 경우의 기본 상한. 화면에 실제로 그리는 개수(줌별 100~3000)에 맞춘 값. */
    private static final int DEFAULT_LIMIT = 3000;
    /** 한 번에 내려줄 수 있는 최대 개수. 이 이상은 화면에 그려도 의미가 없고 응답만 커진다. */
    private static final int MAX_LIMIT = 20000;

    private final WfsService wfsService;

    /** 레이어별 조회를 controller 에서 한 곳으로 모으기 위한 함수형 타입. */
    @FunctionalInterface
    private interface WfsQuery {
        String run(double[] bbox, int limit) throws Exception;
    }

    @GetMapping("/convenience-store")
    public ResponseEntity<String> convenienceStore(
            @RequestParam(value = "bbox", required = false) String bbox,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return respond("편의점", bbox, limit, wfsService::convenienceStore);
    }

    @GetMapping("/busStop-info")
    public ResponseEntity<String> busStopInfo(
            @RequestParam(value = "bbox", required = false) String bbox,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return respond("버스정류장", bbox, limit, wfsService::busStopInfo);
    }

    @GetMapping("/cctv-info")
    public ResponseEntity<String> cctvInfo(
            @RequestParam(value = "bbox", required = false) String bbox,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return respond("CCTV", bbox, limit, wfsService::cctvInfo);
    }

    @GetMapping("/pharmacy-info")
    public ResponseEntity<String> pharmacyInfo(
            @RequestParam(value = "bbox", required = false) String bbox,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return respond("약국", bbox, limit, wfsService::pharmacyInfo);
    }

    @GetMapping("/hospital-info")
    public ResponseEntity<String> hospitalInfo(
            @RequestParam(value = "bbox", required = false) String bbox,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return respond("병원", bbox, limit, wfsService::hospitalInfo);
    }

    @GetMapping("/governmentOffice-info")
    public ResponseEntity<String> governmentOfficeInfo(
            @RequestParam(value = "bbox", required = false) String bbox,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return respond("관공서", bbox, limit, wfsService::governmentOfficeInfo);
    }

    /**
     * bbox 를 주면 그 화면 영역만, 주지 않으면 기존처럼 전국 전체를 내려준다.
     *
     * 조회 실패 시 HTTP 200 + 빈 바디를 반환하는 것은 이 컨트롤러의 기존 동작이라 그대로 두었다
     * (프론트가 이 형태를 전제로 예외 처리하고 있음). 대신 원인은 로그로 남긴다.
     */
    private ResponseEntity<String> respond(String layerName, String bboxParam, Integer limitParam, WfsQuery query) {
        double[] bbox;
        try {
            bbox = parseBbox(bboxParam);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"" + e.getMessage() + "\"}");
        }

        int limit = clampLimit(limitParam);
        String geojson = "";
        try {
            long startedAt = System.currentTimeMillis();
            geojson = query.run(bbox, limit);
            if (bbox != null) {
                log.info("{} bbox 조회 완료: {}ms, {}바이트", layerName,
                        System.currentTimeMillis() - startedAt, geojson == null ? 0 : geojson.length());
            }
        } catch (Exception e) {
            log.error("{} 조회 실패 (bbox={}, limit={})", layerName, bboxParam, limit, e);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .body(geojson == null ? "" : geojson);
    }

    /** "minX,minY,maxX,maxY"(EPSG:3857) 파싱. 값이 없으면 null(=전체 조회). */
    private double[] parseBbox(String bboxParam) {
        if (bboxParam == null || bboxParam.isBlank()) {
            return null;
        }
        String[] parts = bboxParam.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("bbox must be 'minX,minY,maxX,maxY'");
        }
        double[] values = new double[4];
        for (int i = 0; i < 4; i++) {
            try {
                values[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bbox must contain 4 numbers");
            }
            if (!Double.isFinite(values[i])) {
                throw new IllegalArgumentException("bbox must contain 4 finite numbers");
            }
        }
        if (values[0] >= values[2] || values[1] >= values[3]) {
            throw new IllegalArgumentException("bbox must satisfy minX < maxX and minY < maxY");
        }
        return values;
    }

    private int clampLimit(Integer limitParam) {
        if (limitParam == null || limitParam <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limitParam, MAX_LIMIT);
    }
}
