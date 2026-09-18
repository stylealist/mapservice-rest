package com.example.mapservice.controller;

import com.example.mapservice.service.QfieldOfficeWorkPhotoService;
import com.example.mapservice.service.QfieldOfficeWorkService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 시설물 내업 기록의 처리 전·후 사진 API — map.facility_office_work_photo.
 *
 * 사진은 DB 에 바이너리로 저장한다. 구분(BEFORE/AFTER)별 최대 5장, 장당 10MB, JPEG·PNG·WebP 만
 * (파일 시그니처로 확인). 내업 기록·사진이 없으면 404, 입력 오류 400, 장수 초과 409, 크기 초과 413.
 * 쓰기 응답과 목록은 캐시하지 않고(no-store), 이미지 본문은 바뀌지 않으므로 1시간 캐시한다.
 */
@RestController
@RequiredArgsConstructor
public class QfieldOfficeWorkPhotoController {

    private final QfieldOfficeWorkPhotoService qfieldOfficeWorkPhotoService;

    @PostMapping(value = "/qfield/office-works/{workId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadPhoto(
            @PathVariable("workId") String workId,
            @RequestParam(value = "kind", required = false) String kind,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        Long id = parseId(workId);
        if (id == null) {
            return message(HttpStatus.BAD_REQUEST, "workId must be a positive number");
        }

        try {
            String json = qfieldOfficeWorkPhotoService.uploadPhoto(id, kind, file);
            if (json == null) {
                return message(HttpStatus.NOT_FOUND, "Office work not found: " + id);
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(json);
        } catch (QfieldOfficeWorkService.ValidationException e) {
            return message(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (QfieldOfficeWorkPhotoService.LimitExceededException e) {
            return message(HttpStatus.CONFLICT, e.getMessage());
        } catch (QfieldOfficeWorkPhotoService.FileTooLargeException e) {
            return message(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
        }
    }

    @GetMapping("/qfield/office-works/{workId}/photos")
    public ResponseEntity<String> getPhotos(@PathVariable("workId") String workId) {
        Long id = parseId(workId);
        if (id == null) {
            return message(HttpStatus.BAD_REQUEST, "workId must be a positive number");
        }

        String json = qfieldOfficeWorkPhotoService.getPhotos(id);
        if (json == null) {
            return message(HttpStatus.NOT_FOUND, "Office work not found: " + id);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(json);
    }

    @GetMapping("/qfield/office-works/{workId}/photos/{photoId}")
    public ResponseEntity<?> getPhoto(
            @PathVariable("workId") String workId,
            @PathVariable("photoId") String photoId) {

        Long id = parseId(workId);
        Long pid = parseId(photoId);
        if (id == null || pid == null) {
            return message(HttpStatus.BAD_REQUEST, "workId and photoId must be positive numbers");
        }

        QfieldOfficeWorkPhotoService.PhotoContent photo = qfieldOfficeWorkPhotoService.getPhotoContent(id, pid);
        if (photo == null) {
            return message(HttpStatus.NOT_FOUND, "Photo not found: " + pid);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.mimeType()))
                .contentLength(photo.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(photo.fileName(), StandardCharsets.UTF_8).build().toString())
                // 사진 내용은 바뀌지 않는다(교체는 삭제 후 새 photo_id 로 등록)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .header("X-Content-Type-Options", "nosniff")
                .body(photo.content());
    }

    @DeleteMapping("/qfield/office-works/{workId}/photos/{photoId}")
    public ResponseEntity<String> deletePhoto(
            @PathVariable("workId") String workId,
            @PathVariable("photoId") String photoId) {

        Long id = parseId(workId);
        Long pid = parseId(photoId);
        if (id == null || pid == null) {
            return message(HttpStatus.BAD_REQUEST, "workId and photoId must be positive numbers");
        }

        if (!qfieldOfficeWorkPhotoService.deletePhoto(id, pid)) {
            return message(HttpStatus.NOT_FOUND, "Photo not found: " + pid);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    private Long parseId(String value) {
        if (value == null || !value.matches("^[0-9]{1,18}$")) {
            return null;
        }
        long id = Long.parseLong(value);
        return id > 0 ? id : null;
    }

    private ResponseEntity<String> message(HttpStatus status, String text) {
        String json = JsonNodeFactory.instance.objectNode().put("message", text).toString();
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(json);
    }
}
