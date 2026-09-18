package com.example.mapservice.service;

import java.util.List;

public interface QfieldReportService {

    /**
     * 특정 시설물의 내업 처리 결과 보고서 PDF 바이너리 생성
     *
     * @param totalId 시설물 ID
     * @param workId  특정 내업 ID (null이면 최신 완료/보류 기록)
     * @return PDF 바이트 배열
     */
    byte[] generateFacilityReportPdf(String totalId, Long workId);

    /**
     * 단일 시설물 보고서 다운로드용 파일명 생성 (확장자 .pdf 포함)
     */
    String getFacilityReportFileName(String totalId, Long workId);

    /**
     * 조건별 시설물 내업 현황 일괄 보고서 PDF 바이너리 생성
     *
     * @param statuses 대상 상태 목록 (예: DONE, HOLD)
     * @param sidoCd   시도 코드
     * @param sggCd    시군구 코드
     * @param emdCd    읍면동 코드
     * @param keyword  검색어
     * @return PDF 바이트 배열
     */
    byte[] generateFacilityListReportPdf(List<String> statuses, String sidoCd, String sggCd, String emdCd, String keyword);

    /**
     * 일괄 현황 보고서 다운로드용 파일명 생성
     */
    String getFacilityListReportFileName(List<String> statuses);
}
