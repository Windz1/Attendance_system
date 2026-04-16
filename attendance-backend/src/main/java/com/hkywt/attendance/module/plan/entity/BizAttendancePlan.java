package com.hkywt.attendance.module.plan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("biz_attendance_plan")
public class BizAttendancePlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String planName;
    private Integer planType;
    private Integer status;
    private String timezone;
    private Integer dateMode;
    private String weekdays;
    private LocalTime windowStartTime;
    private LocalTime windowEndTime;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private Integer allowMemberModifyAfterSubmit;
    private String remark;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
