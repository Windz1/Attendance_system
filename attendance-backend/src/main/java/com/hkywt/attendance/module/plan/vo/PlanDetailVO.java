package com.hkywt.attendance.module.plan.vo;

import com.hkywt.attendance.module.plan.entity.BizAttendancePlan;
import lombok.Data;

import java.util.List;

@Data
public class PlanDetailVO {
    private BizAttendancePlan plan;
    private List<Long> classIds;
}
