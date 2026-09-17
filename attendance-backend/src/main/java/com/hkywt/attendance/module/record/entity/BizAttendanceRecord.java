package com.hkywt.attendance.module.record.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_attendance_record")
public class BizAttendanceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long planId;
    private Long classId;
    private Long studentId;
    private Integer attendanceStatus;
    private Integer isDefaultMarked;
    private String remark;
    private String classPhotoUrl;
    private Long operatorUserId;
    private LocalDateTime operateTime;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
