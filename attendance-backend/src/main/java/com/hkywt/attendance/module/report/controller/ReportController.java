package com.hkywt.attendance.module.report.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.report.service.ReportService;
import com.hkywt.attendance.module.report.vo.ClassStatusStatVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/class-status")
    public ApiResult<List<ClassStatusStatVO>> classStatus(@RequestParam(required = false) LocalDate startDate,
                                                           @RequestParam(required = false) LocalDate endDate,
                                                           @RequestParam(required = false) Long classId) {
        return ApiResult.success(reportService.classStatusStat(startDate, endDate, classId));
    }
}
