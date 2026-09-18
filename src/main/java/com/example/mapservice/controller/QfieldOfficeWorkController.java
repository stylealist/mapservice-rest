package com.example.mapservice.controller;

import com.example.mapservice.service.QfieldOfficeWorkService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 시설물 내업(사무실 처리) 기록 API — map.facility_office_work.
 *
 * 외업(QField)에서 보수 요청된 시설물에 대해 내업에서 처리 상태·담당·일정·비용 등을 기록한다.
 * 시설물이 없으면 404, 입력 검증 실패는 400. 쓰기 응답은 캐시하지 않는다(no-store).
 */
@RestController
@RequiredArgsConstructor
public class QfieldOfficeWorkController {

    private final QfieldOfficeWorkService qfieldOfficeWorkService;

    @GetMapping("/qfield/facilities/{totalId}/office-works")
    public ResponseEntity<String> getOfficeWorks(@PathVariable("totalId") String totalId) {
        if (totalId == null || totalId.isBlank()) {
            return message(HttpStatus.BAD_REQUEST, "totalId cannot be blank");
        }

        String json = qfieldOfficeWorkService.getOfficeWorks(totalId.trim());
        if (json == null) {
            return message(HttpStatus.NOT_FOUND, "Facility not found: " + totalId.trim());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(json);
    }

    @PostMapping("/qfield/facilities/{totalId}/office-works")
    public ResponseEntity<String> createOfficeWork(
            @PathVariable("totalId") String totalId,
            @RequestBody(required = false) Map<String, Object> body) {

        if (totalId == null || totalId.isBlank()) {
            return message(HttpStatus.BAD_REQUEST, "totalId cannot be blank");
        }

        try {
            String json = qfieldOfficeWorkService.createOfficeWork(totalId.trim(), body);
            if (json == null) {
                return message(HttpStatus.NOT_FOUND, "Facility not found: " + totalId.trim());
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(json);
        } catch (QfieldOfficeWorkService.ValidationException e) {
            return message(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/qfield/office-works/{workId}")
    public ResponseEntity<String> updateOfficeWork(
            @PathVariable("workId") String workId,
            @RequestBody(required = false) Map<String, Object> body) {

        Long id = parseWorkId(workId);
        if (id == null) {
            return message(HttpStatus.BAD_REQUEST, "workId must be a positive number");
        }

        try {
            String json = qfieldOfficeWorkService.updateOfficeWork(id, body);
            if (json == null) {
                return message(HttpStatus.NOT_FOUND, "Office work not found: " + id);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(json);
        } catch (QfieldOfficeWorkService.ValidationException e) {
            return message(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/qfield/office-works/{workId}")
    public ResponseEntity<String> deleteOfficeWork(@PathVariable("workId") String workId) {
        Long id = parseWorkId(workId);
        if (id == null) {
            return message(HttpStatus.BAD_REQUEST, "workId must be a positive number");
        }

        if (!qfieldOfficeWorkService.deleteOfficeWork(id)) {
            return message(HttpStatus.NOT_FOUND, "Office work not found: " + id);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    private Long parseWorkId(String workId) {
        if (workId == null || !workId.matches("^[0-9]{1,18}$")) {
            return null;
        }
        long id = Long.parseLong(workId);
        return id > 0 ? id : null;
    }

    private ResponseEntity<String> message(HttpStatus status, String text) {
        // 경로 값이 메시지에 섞이므로 문자열 연결 대신 JSON 직렬화로 이스케이프한다
        String json = JsonNodeFactory.instance.objectNode().put("message", text).toString();
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(json);
    }
}
