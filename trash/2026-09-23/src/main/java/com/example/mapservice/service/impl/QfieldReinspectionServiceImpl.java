package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.QfieldReinspectionMapper;
import com.example.mapservice.service.QfieldReinspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class QfieldReinspectionServiceImpl implements QfieldReinspectionService {

    private static final Set<String> updatableStatuses = Set.of("REQUESTED", "CONFIRMED", "CANCELED");
    private static final int reasonMaxLength = 500;
    private static final int userMaxLength = 100;

    // 테이블이 없다고 판단한 뒤 다시 확인하기까지의 간격.
    // 담당자가 db/map_facility_reinspection.sql 을 실행하면 재기동 없이 이 시간 안에 반영된다.
    private static final long tableRecheckMillis = 60_000L;

    private static final String emptyItems = "{\"items\":[]}";

    private final QfieldReinspectionMapper mapper;

    // 재점검 테이블 존재 여부 캐시 (내업 테이블과 같은 방식 — QfieldFacilityServiceImpl 참고)
    private volatile boolean tableExists;
    private volatile long tableCheckedAt;

    /**
     * 목록 화면용 — 열린 요청만. 테이블이 없으면 빈 목록을 돌려준다.
     * 시설물 목록 API 와 달리 이 응답이 없어도 지도는 정상 동작해야 하므로 예외를 올리지 않는다.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String getOpenReinspections(String sidoCd, String sggCd, String emdCd) {
        if (!isTableAvailable()) {
            return emptyItems;
        }
        try {
            return mapper.getOpenReinspections(resolveCode(sidoCd, sggCd, emdCd));
        } catch (RuntimeException e) {
            log.warn("재점검 요청 목록 조회 실패 — 배지 없이 표시됩니다. 원인: {}", e.getMessage());
            updateTableState(false);
            return emptyItems;
        }
    }

    @Override
    public String getReinspections(String totalId) {
        requireTable();
        if (mapper.getFacilityRepairYn(totalId) == null) {
            return null;
        }
        return mapper.getReinspections(totalId);
    }

    /**
     * 재점검 요청 등록. 보수 필요(repair_required_yn='Y') 시설물만 대상이며,
     * 열린 요청(REQUESTED)은 시설물당 하나만 둘 수 있다.
     */
    @Override
    @Transactional
    public String createReinspection(String totalId, Map<String, Object> body) {
        requireTable();
        Map<String, Object> item = validateCreate(body);

        String repairYn = mapper.getFacilityRepairYn(totalId);
        if (repairYn == null) {
            return null;
        }
        if (!"Y".equalsIgnoreCase(repairYn.trim())) {
            throw new ValidationException("reinspection can only be requested for facilities with repair_required_yn = Y");
        }
        if (mapper.findOpenReinspectId(totalId) != null) {
            throw new DuplicateRequestException("a reinspection request is already open for this facility");
        }

        try {
            return mapper.insertReinspection(totalId, item);
        } catch (DuplicateKeyException e) {
            // 동시에 두 요청이 들어온 경우 — DB 의 부분 UNIQUE 인덱스가 막아준다
            throw new DuplicateRequestException("a reinspection request is already open for this facility");
        }
    }

    @Override
    @Transactional
    public String updateReinspection(long reinspectId, Map<String, Object> body) {
        requireTable();
        try {
            return mapper.updateReinspection(reinspectId, validateUpdate(body));
        } catch (DuplicateKeyException e) {
            // 이미 열린 요청이 있는 시설물의 기록을 다시 REQUESTED 로 되돌리려는 경우
            throw new DuplicateRequestException("another reinspection request is already open for this facility");
        }
    }

    @Override
    @Transactional
    public boolean deleteReinspection(long reinspectId) {
        requireTable();
        return mapper.deleteReinspection(reinspectId) > 0;
    }

    /** 가장 구체적인 행정구역 코드 하나만 적용 (emdCd > sggCd > sidoCd) */
    private String resolveCode(String sidoCd, String sggCd, String emdCd) {
        if (emdCd != null && !emdCd.isBlank()) {
            return emdCd.trim();
        }
        if (sggCd != null && !sggCd.isBlank()) {
            return sggCd.trim();
        }
        if (sidoCd != null && !sidoCd.isBlank()) {
            return sidoCd.trim();
        }
        return null;
    }

    private Map<String, Object> validateCreate(Map<String, Object> body) {
        Map<String, Object> item = new HashMap<>();
        Map<String, Object> source = body == null ? Map.of() : body;

        item.put("reason", toText(source.get("reason"), "reason", reasonMaxLength));
        item.put("request_user", toText(source.get("request_user"), "request_user", userMaxLength));
        item.put("work_id", toWorkId(source.get("work_id")));
        return item;
    }

    private Map<String, Object> validateUpdate(Map<String, Object> body) {
        if (body == null) {
            throw new ValidationException("request body is required");
        }
        Map<String, Object> item = new HashMap<>();

        String status = toText(body.get("status"), "status", 20);
        if (status == null) {
            throw new ValidationException("status is required");
        }
        status = status.toUpperCase();
        if (!updatableStatuses.contains(status)) {
            throw new ValidationException("status must be one of REQUESTED, CONFIRMED, CANCELED");
        }
        item.put("status", status);
        item.put("reason", toText(body.get("reason"), "reason", reasonMaxLength));
        item.put("confirm_user", toText(body.get("confirm_user"), "confirm_user", userMaxLength));
        item.put("confirm_note", toText(body.get("confirm_note"), "confirm_note", reasonMaxLength));
        return item;
    }

    private Long toWorkId(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        if (!text.matches("^[0-9]{1,18}$")) {
            throw new ValidationException("work_id must be a positive number");
        }
        long workId = Long.parseLong(text);
        if (workId <= 0) {
            throw new ValidationException("work_id must be a positive number");
        }
        return workId;
    }

    /** 빈 문자열은 null 로 본다(내업 기록과 같은 규칙) */
    private String toText(Object value, String key, int maxLength) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String) && !(value instanceof Number)) {
            throw new ValidationException(key + " must be a string");
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > maxLength) {
            throw new ValidationException(key + " must be at most " + maxLength + " characters");
        }
        return text;
    }

    private void requireTable() {
        if (!isTableAvailable()) {
            throw new NotConfiguredException("reinspection table (map.facility_reinspection) does not exist");
        }
    }

    private boolean isTableAvailable() {
        if (tableExists) {
            return true;
        }
        long checkedAt = tableCheckedAt;
        if (checkedAt != 0 && System.currentTimeMillis() - checkedAt < tableRecheckMillis) {
            return false;
        }
        boolean exists;
        try {
            exists = mapper.existsReinspectionTable();
        } catch (RuntimeException e) {
            exists = false;
        }
        updateTableState(exists);
        return exists;
    }

    // 상태가 바뀔 때만 한 줄 남긴다(매 요청·매 재확인마다 찍지 않음)
    private synchronized void updateTableState(boolean exists) {
        boolean firstCheck = tableCheckedAt == 0;
        if (exists && !tableExists && !firstCheck) {
            log.info("재점검 테이블(map.facility_reinspection)이 확인되어 재점검 기능을 사용합니다.");
        } else if (!exists && (tableExists || firstCheck)) {
            log.warn("재점검 테이블(map.facility_reinspection)이 없어 재점검 기능이 비활성화됩니다(목록은 빈 배열, 등록은 503). "
                    + "db/map_facility_reinspection.sql 을 실행하세요.");
        }
        tableExists = exists;
        tableCheckedAt = System.currentTimeMillis();
    }
}
