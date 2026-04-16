package com.hkywt.attendance.module.system.service;

import java.util.List;

public interface DataMaintenanceService {
    /**
     * 换届清空业务数据（班级、学生及考勤业务表）
     */
    void turnoverReset(List<String> targets);
}
