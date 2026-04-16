package com.hkywt.attendance.module.plan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_attendance_plan_class")
public class BizAttendancePlanClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long classId;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
