package com.hkywt.attendance.module.record.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.record.service.RecordService;
import com.hkywt.attendance.module.record.vo.RecordListVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public ApiResult<List<RecordListVO>> query(@RequestParam(required = false) String startTime,
                                               @RequestParam(required = false) String endTime,
                                               @RequestParam(required = false) LocalDate startDate,
                                               @RequestParam(required = false) LocalDate endDate,
                                               @RequestParam(required = false) Long classId,
                                               @RequestParam(required = false) Long planId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) Boolean leaveOnly,
                                               @RequestParam(required = false) String sortField,
                                               @RequestParam(required = false) String sortOrder) {
        LocalDateTime start = parseDateTime(startTime, true);
        LocalDateTime end = parseDateTime(endTime, false);
        if (start == null && startDate != null) start = startDate.atStartOfDay();
        if (end == null && endDate != null) end = endDate.atTime(LocalTime.MAX);
        return ApiResult.success(recordService.query(start, end, classId, planId, keyword, status, leaveOnly, sortField, sortOrder));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       @RequestParam(required = false) LocalDate startDate,
                       @RequestParam(required = false) LocalDate endDate,
                       @RequestParam(required = false) Long classId,
                       @RequestParam(required = false) Long planId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Boolean leaveOnly,
                       @RequestParam(required = false) String sortField,
                       @RequestParam(required = false) String sortOrder,
                       HttpServletResponse response) {
        LocalDateTime start = parseDateTime(startTime, true);
        LocalDateTime end = parseDateTime(endTime, false);
        if (start == null && startDate != null) start = startDate.atStartOfDay();
        if (end == null && endDate != null) end = endDate.atTime(LocalTime.MAX);
        recordService.export(start, end, classId, planId, keyword, status, leaveOnly, sortField, sortOrder, response);
    }

    private LocalDateTime parseDateTime(String val, boolean start) {
        if (val == null || val.isBlank()) return null;
        String s = val.trim();
        try {
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return start ? d.atStartOfDay() : d.atTime(LocalTime.MAX);
            }
            if (s.length() == 19 && s.contains(" ")) {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
