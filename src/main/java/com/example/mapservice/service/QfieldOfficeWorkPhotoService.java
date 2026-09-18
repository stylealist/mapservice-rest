package com.example.mapservice.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 시설물 내업 기록의 처리 전·후 사진 — map.facility_office_work_photo (DB 에 바이너리로 저장).
 * 대상(내업 기록·사진)이 없으면 null/false 를 돌려준다(컨트롤러가 404 처리).
 * 입력 오류는 {@link QfieldOfficeWorkService.ValidationException}(400),
 * 구분별 장수 초과는 {@link LimitExceededException}(409), 파일 크기 초과는 {@link FileTooLargeException}(413).
 */
public interface QfieldOfficeWorkPhotoService {

    /** 구분(BEFORE/AFTER)별 최대 장수 */
    int maxPhotosPerKind = 5;
    /** 장당 최대 크기(바이트). application.yml 의 multipart max-file-size 와 맞출 것 */
    long maxFileSize = 10L * 1024 * 1024;

    String uploadPhoto(long workId, String kind, MultipartFile file);
    String getPhotos(long workId);
    PhotoContent getPhotoContent(long workId, long photoId);
    boolean deletePhoto(long workId, long photoId);

    record PhotoContent(String fileName, String mimeType, byte[] content) {
    }

    class LimitExceededException extends RuntimeException {
        public LimitExceededException(String message) {
            super(message);
        }
    }

    class FileTooLargeException extends RuntimeException {
        public FileTooLargeException(String message) {
            super(message);
        }
    }
}
