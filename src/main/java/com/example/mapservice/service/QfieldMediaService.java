package com.example.mapservice.service;

/**
 * QField 시설물 첨부 파일(사진·음성·영상) 중계.
 *
 * DB에는 URL이 아니라 QField 프로젝트 안의 상대 경로(DCIM/x.jpg 등)만 저장되고,
 * 원본은 QFieldCloud에 있으며 API가 인증을 요구한다. 브라우저는 인증 헤더를 붙일 수 없으므로
 * 백엔드가 대신 받아 전달한다.
 */
public interface QfieldMediaService {

    /** 중계 결과 — 파일 바이트와 표시용 정보 */
    record MediaContent(byte[] body, String contentType, String fileName) {}

    /** 중계 실패 사유 */
    enum MediaError {
        NOT_CONFIGURED,   // QFieldCloud 접속 정보 미설정
        FACILITY_NOT_FOUND,
        PATH_NOT_ALLOWED, // 해당 시설물이 가진 첨부가 아님
        PROJECT_NOT_FOUND,
        FILE_NOT_FOUND,
        UPSTREAM_ERROR
    }

    /** 중계 예외 — 컨트롤러가 사유별 상태 코드로 변환한다 */
    class MediaException extends RuntimeException {
        private final MediaError error;

        public MediaException(MediaError error, String message) {
            super(message);
            this.error = error;
        }

        public MediaError getError() {
            return error;
        }
    }

    /**
     * 시설물이 실제로 가진 첨부인지 확인한 뒤 QFieldCloud에서 파일을 받아 돌려준다.
     *
     * @param totalId 시설물 식별자
     * @param path    프로젝트 내 상대 경로 (예: DCIM/JPEG_20260916071830596.jpg)
     */
    MediaContent getFacilityMedia(String totalId, String path);
}
