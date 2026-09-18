package com.example.mapservice.controller;

import com.example.mapservice.service.QfieldFacilityService;
import com.example.mapservice.service.QfieldMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class QfieldFacilityController {

    private final QfieldFacilityService qfieldFacilityService;
    private final QfieldMediaService qfieldMediaService;

    @GetMapping("/qfield/facilities")
    public ResponseEntity<String> getFacilities(
            @RequestParam(value = "sidoCd", required = false) String sidoCd,
            @RequestParam(value = "sggCd", required = false) String sggCd,
            @RequestParam(value = "emdCd", required = false) String emdCd) {

        if (sidoCd != null && !sidoCd.isEmpty() && !sidoCd.matches("^[0-9]{2}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"sidoCd must be a 2-digit number\"}");
        }
        if (sggCd != null && !sggCd.isEmpty() && !sggCd.matches("^[0-9]{5}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"sggCd must be a 5-digit number\"}");
        }
        if (emdCd != null && !emdCd.isEmpty() && !emdCd.matches("^[0-9]{8}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"emdCd must be an 8-digit number\"}");
        }

        String geojson = qfieldFacilityService.getFacilities(sidoCd, sggCd, emdCd);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .body(geojson != null ? geojson : "{\"type\":\"FeatureCollection\",\"features\":[]}");
    }

    @GetMapping("/qfield/facilities/{totalId}")
    public ResponseEntity<String> getFacilityDetail(@PathVariable("totalId") String totalId) {
        if (totalId == null || totalId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"totalId cannot be blank\"}");
        }

        String geojson = qfieldFacilityService.getFacilityDetail(totalId.trim());
        if (geojson == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"Facility not found: " + totalId.trim() + "\"}");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .body(geojson);
    }

    /**
     * 시설물 첨부 파일(사진·음성·영상) 중계.
     *
     * DB에는 QField 프로젝트 안의 상대 경로만 있고 원본은 QFieldCloud에 있으며 인증이 필요하다.
     * 브라우저는 인증 헤더를 붙일 수 없으므로 백엔드가 대신 받아 전달한다.
     * 요청된 경로가 그 시설물의 첨부인지 먼저 확인해 임의 파일 접근을 막는다.
     */
    @GetMapping("/qfield/facilities/{totalId}/media")
    public ResponseEntity<byte[]> getFacilityMedia(
            @PathVariable("totalId") String totalId,
            @RequestParam("path") String path) {

        if (totalId == null || totalId.isBlank() || path == null || path.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            QfieldMediaService.MediaContent media = qfieldMediaService.getFacilityMedia(totalId.trim(), path.trim());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, media.contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.fileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                    .body(media.body());
        } catch (QfieldMediaService.MediaException e) {
            log.warn("시설물 첨부 중계 실패({}): {}", e.getError(), e.getMessage());
            return ResponseEntity.status(toStatus(e.getError())).build();
        }
    }

    private HttpStatus toStatus(QfieldMediaService.MediaError error) {
        return switch (error) {
            case NOT_CONFIGURED -> HttpStatus.SERVICE_UNAVAILABLE;
            case FACILITY_NOT_FOUND, FILE_NOT_FOUND, PROJECT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PATH_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    /**
     * 시설물 지도 아이콘 설정 조회.
     * 설정 테이블(map.facility_icon)이 아직 없거나 조회에 실패하면 빈 배열을 돌려주고,
     * 프론트엔드는 내장 기본 아이콘으로 표시한다.
     */
    @GetMapping("/qfield/facility-icons")
    public ResponseEntity<String> getFacilityIcons() {
        String json = qfieldFacilityService.getFacilityIcons();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(json != null ? json : "[]");
    }

    @GetMapping("/admin-area/sido")
    public ResponseEntity<String> getSidoList() {
        String json = qfieldFacilityService.getSidoList();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(json != null ? json : "[]");
    }

    @GetMapping("/admin-area/sgg")
    public ResponseEntity<String> getSggList(@RequestParam(value = "sidoCd", required = false) String sidoCd) {
        if (sidoCd == null || !sidoCd.matches("^[0-9]{2}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"sidoCd must be a 2-digit number\"}");
        }

        String json = qfieldFacilityService.getSggList(sidoCd);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(json != null ? json : "[]");
    }

    @GetMapping("/admin-area/emd")
    public ResponseEntity<String> getEmdList(@RequestParam(value = "sggCd", required = false) String sggCd) {
        if (sggCd == null || !sggCd.matches("^[0-9]{5}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"sggCd must be a 5-digit number\"}");
        }

        String json = qfieldFacilityService.getEmdList(sggCd);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(json != null ? json : "[]");
    }
}
