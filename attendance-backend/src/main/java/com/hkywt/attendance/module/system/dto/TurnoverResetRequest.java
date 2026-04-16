package com.hkywt.attendance.module.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class TurnoverResetRequest {
    /**
     * 可选值：
     * STUDENT_DATA, CLASS_DATA, PLAN_DATA, TASK_DATA, RECORD_DATA, IMPORT_LOG_DATA
     */
    private List<String> targets;
}
