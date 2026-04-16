package com.hkywt.attendance.module.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_attendance_task")
public class BizAttendanceTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private Long planId;
    private Long classId;
    private LocalDate taskDate;
    private LocalDateTime windowStartAt;
    private LocalDateTime windowEndAt;
    private Integer taskStatus;
    private Long submitterUserId;
    private LocalDateTime submittedAt;
    private Integer allowModify;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
