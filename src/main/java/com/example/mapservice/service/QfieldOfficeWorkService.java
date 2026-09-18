package com.example.mapservice.service;

import java.util.Map;

/**
 * 시설물 내업(사무실 처리) 기록 — map.facility_office_work.
 * 조회·등록·수정은 JSON 문자열을 돌려주며, 대상이 없으면 null 을 돌려준다(컨트롤러가 404 처리).
 * 등록·수정 본문이 형식에 맞지 않으면 {@link ValidationException} 을 던진다(컨트롤러가 400 처리).
 */
public interface QfieldOfficeWorkService {
    String getOfficeWorks(String totalId);
    String createOfficeWork(String totalId, Map<String, Object> body);
    String updateOfficeWork(long workId, Map<String, Object> body);
    boolean deleteOfficeWork(long workId);

    class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
