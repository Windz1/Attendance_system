package com.hkywt.attendance.module.plan.service;

import com.hkywt.attendance.module.plan.dto.PlanSaveRequest;
import com.hkywt.attendance.module.plan.entity.BizAttendancePlan;
import com.hkywt.attendance.module.plan.vo.PlanDetailVO;

import java.util.List;

public interface PlanService {
    void save(PlanSaveRequest request);
    void changeStatus(Long id, Integer status);
    void delete(Long id);
    List<BizAttendancePlan> list();
    PlanDetailVO getDetail(Long id);
}
