package com.hkywt.attendance.module.system.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.system.dto.TurnoverResetRequest;
import com.hkywt.attendance.module.system.service.DataMaintenanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system/maintenance")
public class DataMaintenanceController {

    private final DataMaintenanceService dataMaintenanceService;

    public DataMaintenanceController(DataMaintenanceService dataMaintenanceService) {
        this.dataMaintenanceService = dataMaintenanceService;
    }

    @PostMapping("/turnover-reset")
    public ApiResult<Void> turnoverReset(@RequestBody TurnoverResetRequest request) {
        dataMaintenanceService.turnoverReset(request == null ? null : request.getTargets());
        return ApiResult.success();
    }

    @GetMapping("/record-retention")
    public ApiResult<Map<String, Object>> retentionPolicy() {
        return ApiResult.success(Map.of(
                "retentionMonths", 1,
                "cleanupTime", "每天 03:10",
                "description", "考勤记录仅保留最近1个月，超过时间自动清理"
        ));
    }
}
