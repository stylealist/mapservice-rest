package com.example.mapservice.service;

import java.util.Map;

/**
 * 시설물 현장 재점검 요청 — map.facility_reinspection.
 *
 * 내업 완료는 "사무실 처리가 끝났다"는 뜻이고, 현장 보수가 확인된 것은 아니다.
 * 보수 필요 여부(repair_required_yn)는 외업이 채우는 값이라 고치지 않고,
 * "처리 끝, 현장 확인만 남음" 상태를 이 기능으로 따로 남긴다.
 *
 * 조회·등록·수정은 JSON 문자열을 돌려주며, 대상이 없으면 null(컨트롤러가 404)이다.
 * 본문 형식 오류는 {@link ValidationException}(400), 이미 열린 요청이 있으면
 * {@link DuplicateRequestException}(409), 테이블이 없으면 {@link NotConfiguredException}(503)이다.
 */
public interface QfieldReinspectionService {

    /** 행정구역 범위의 열린 요청 목록. 테이블이 없으면 빈 items 를 돌려준다(지도는 정상 동작). */
    String getOpenReinspections(String sidoCd, String sggCd, String emdCd);

    String getReinspections(String totalId);

    String createReinspection(String totalId, Map<String, Object> body);

    String updateReinspection(long reinspectId, Map<String, Object> body);

    boolean deleteReinspection(long reinspectId);

    class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    class DuplicateRequestException extends RuntimeException {
        public DuplicateRequestException(String message) {
            super(message);
        }
    }

    class NotConfiguredException extends RuntimeException {
        public NotConfiguredException(String message) {
            super(message);
        }
    }
}
