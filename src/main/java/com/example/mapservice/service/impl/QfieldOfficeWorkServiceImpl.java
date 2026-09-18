package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.QfieldOfficeWorkMapper;
import com.example.mapservice.service.QfieldOfficeWorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QfieldOfficeWorkServiceImpl implements QfieldOfficeWorkService {

    private static final Set<String> workStatuses = Set.of("RECEIVED", "IN_PROGRESS", "DONE", "HOLD");

    // 문자열 컬럼과 DB 최대 길이(text 컬럼은 Integer.MAX_VALUE)
    private static final Map<String, Integer> textColumns = Map.ofEntries(
            Map.entry("work_content", Integer.MAX_VALUE),
            Map.entry("dept_nm", 100),
            Map.entry("manager_nm", 50),
            Map.entry("manager_tel", 50),
            Map.entry("vendor_nm", 200),
            Map.entry("contract_no", 100),
            Map.entry("before_photo", Integer.MAX_VALUE),
            Map.entry("after_photo", Integer.MAX_VALUE),
            Map.entry("remark", Integer.MAX_VALUE)
    );

    private final QfieldOfficeWorkMapper mapper;

    @Override
    public String getOfficeWorks(String totalId) {
        if (mapper.getFacilityRepairYn(totalId) == null) {
            return null;
        }
        return mapper.getOfficeWorks(totalId);
    }

    /** 내업은 보수 필요(repair_required_yn='Y') 시설물에만 작성할 수 있다. */
    @Override
    @Transactional
    public String createOfficeWork(String totalId, Map<String, Object> body) {
        Map<String, Object> work = validate(body);
        String repairYn = mapper.getFacilityRepairYn(totalId);
        if (repairYn == null) {
            return null;
        }
        if (!"Y".equalsIgnoreCase(repairYn.trim())) {
            throw new ValidationException("office work can only be created for facilities with repair_required_yn = Y");
        }
        return mapper.insertOfficeWork(totalId, work);
    }

    @Override
    @Transactional
    public String updateOfficeWork(long workId, Map<String, Object> body) {
        return mapper.updateOfficeWork(workId, validate(body));
    }

    @Override
    @Transactional
    public boolean deleteOfficeWork(long workId) {
        return mapper.deleteOfficeWork(workId) > 0;
    }

    /** 본문을 검증해 DB 바인딩용 값(LocalDate, BigDecimal, String)으로 바꾼다. 빈 문자열은 null 로 본다. */
    private Map<String, Object> validate(Map<String, Object> body) {
        if (body == null) {
            throw new ValidationException("request body is required");
        }
        Map<String, Object> work = new HashMap<>();

        String workStatus = toText(body.get("work_status"), "work_status");
        if (workStatus == null || !workStatuses.contains(workStatus)) {
            throw new ValidationException("work_status must be one of RECEIVED, IN_PROGRESS, DONE, HOLD");
        }
        work.put("work_status", workStatus);

        textColumns.forEach((key, maxLength) -> {
            String value = toText(body.get(key), key);
            if (value != null && value.length() > maxLength) {
                throw new ValidationException(key + " must be at most " + maxLength + " characters");
            }
            work.put(key, value);
        });

        work.put("plan_date", toDate(body.get("plan_date"), "plan_date"));
        work.put("complete_date", toDate(body.get("complete_date"), "complete_date"));
        // 프론트와 같은 규칙: 완료 처리에는 완료일이 있어야 한다
        if ("DONE".equals(workStatus) && work.get("complete_date") == null) {
            throw new ValidationException("complete_date is required when work_status is DONE");
        }
        work.put("cost", toCost(body.get("cost")));
        return work;
    }

    private String toText(Object value, String key) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String) && !(value instanceof Number)) {
            throw new ValidationException(key + " must be a string");
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private LocalDate toDate(Object value, String key) {
        String text = toText(value, key);
        if (text == null) {
            return null;
        }
        if (!text.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
            throw new ValidationException(key + " must be YYYY-MM-DD");
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ValidationException(key + " must be a valid date (YYYY-MM-DD)");
        }
    }

    private BigDecimal toCost(Object value) {
        String text = toText(value, "cost");
        if (text == null) {
            return null;
        }
        try {
            BigDecimal cost = new BigDecimal(text);
            // numeric(15,0): 정수 15자리까지
            if (cost.stripTrailingZeros().scale() > 0) {
                throw new ValidationException("cost must be an integer");
            }
            if (cost.abs().compareTo(new BigDecimal("999999999999999")) > 0) {
                throw new ValidationException("cost is too large (max 15 digits)");
            }
            return cost.setScale(0);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ValidationException("cost must be a number");
        }
    }
}
