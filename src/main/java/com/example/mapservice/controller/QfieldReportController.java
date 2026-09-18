package com.example.mapservice.controller;

import com.example.mapservice.service.QfieldReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/qfield")
@Tag(name = "QField Report", description = "시설물 내업 처리 완료/보류 보고서 PDF 다운로드 API")
public class QfieldReportController {

    private final QfieldReportService reportService;

    public QfieldReportController(QfieldReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/facilities/{totalId}/report/pdf")
    @Operation(summary = "단일 시설물 내업 처리 결과보고서 PDF 다운로드",
            description = "내업 처리 상태가 완료(DONE) 또는 보류(HOLD)인 시설물의 상세 결과보고서 PDF를 다운로드합니다.")
    public ResponseEntity<?> getFacilityReportPdf(
            @Parameter(description = "시설물 고유 ID") @PathVariable("totalId") String totalId,
            @Parameter(description = "특정 내업 ID (생략 시 최신 완료/보류 내역)") @RequestParam(value = "workId", required = false) Long workId) {
        try {
            byte[] pdfBytes = reportService.generateFacilityReportPdf(totalId, workId);
            String fileName = reportService.getFacilityReportFileName(totalId, workId);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentLength(pdfBytes.length)
                    .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"보고서 PDF 생성 실패: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    @GetMapping("/facilities/report/pdf")
    @Operation(summary = "조건별 시설물 내업 현황 목록 보고서 PDF 다운로드",
            description = "완료(DONE) 또는 보류(HOLD) 상태인 시설물들의 내업 처리 현황 목록 대장을 일괄 PDF로 다운로드합니다.")
    public ResponseEntity<?> getFacilityListReportPdf(
            @Parameter(description = "조회 대상 상태 (DONE, HOLD, 또는 DONE,HOLD)") @RequestParam(value = "status", defaultValue = "DONE,HOLD") String status,
            @Parameter(description = "시·도 코드") @RequestParam(value = "sidoCd", required = false) String sidoCd,
            @Parameter(description = "시·군·구 코드") @RequestParam(value = "sggCd", required = false) String sggCd,
            @Parameter(description = "읍·면·동 코드") @RequestParam(value = "emdCd", required = false) String emdCd,
            @Parameter(description = "시설물명/주소 검색어") @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            List<String> statuses = Arrays.stream(status.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (statuses.isEmpty()) {
                statuses = List.of("DONE", "HOLD");
            }

            byte[] pdfBytes = reportService.generateFacilityListReportPdf(statuses, sidoCd, sggCd, emdCd, keyword);
            String fileName = reportService.getFacilityListReportFileName(statuses);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentLength(pdfBytes.length)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"현황 보고서 PDF 생성 실패: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
