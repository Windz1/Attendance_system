package com.hkywt.attendance.module.system.service.impl;

import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.common.security.SecurityUtil;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.system.mapper.DataMaintenanceMapper;
import com.hkywt.attendance.module.system.service.DataMaintenanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DataMaintenanceServiceImpl implements DataMaintenanceService {

    private final DataMaintenanceMapper dataMaintenanceMapper;

    public DataMaintenanceServiceImpl(DataMaintenanceMapper dataMaintenanceMapper) {
        this.dataMaintenanceMapper = dataMaintenanceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void turnoverReset(List<String> targets) {
        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) {
            throw new BizException(403, "无权限操作");
        }
        if (targets == null || targets.isEmpty()) {
            throw new BizException("请至少选择一项删除内容");
        }

        Set<String> set = new HashSet<>(targets);
        if (set.contains("CLASS_DATA") && !set.contains("STUDENT_DATA")) {
            throw new BizException("删除班级前请同时勾选删除学生信息");
        }

        // 为避免残留脏关联，若勾选计划/任务，则默认先清理考勤记录
        if (set.contains("RECORD_DATA") || set.contains("PLAN_DATA") || set.contains("TASK_DATA")) {
            dataMaintenanceMapper.deleteAllAttendanceRecords();
        }
        if (set.contains("TASK_DATA") || set.contains("PLAN_DATA")) {
            dataMaintenanceMapper.deleteAllAttendanceTasks();
        }
        if (set.contains("PLAN_DATA")) {
            dataMaintenanceMapper.deleteAllAttendancePlanClasses();
            dataMaintenanceMapper.deleteAllAttendancePlans();
        }
        if (set.contains("IMPORT_LOG_DATA")) {
            dataMaintenanceMapper.deleteAllStudentImportLogs();
        }
        if (set.contains("CLASS_DATA")) {
            dataMaintenanceMapper.deleteAllMemberClasses();
            dataMaintenanceMapper.deleteAllClasses();
        }
        if (set.contains("STUDENT_DATA")) {
            dataMaintenanceMapper.deleteAllStudents();
        }
    }
}
