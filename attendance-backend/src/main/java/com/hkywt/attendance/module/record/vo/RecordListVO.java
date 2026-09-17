package com.hkywt.attendance.module.record.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecordListVO {
    private Long taskId;
    private Long planId;
    private Long classId;
    private Long studentId;

    private String planName;
    private String className;
    private List<String> classPhotoUrls;
    private LocalDateTime windowStartAt;
    private LocalDateTime windowEndAt;
    private LocalDateTime submittedAt;
    private String studentName;
    private String studentNo;
    private Integer attendanceStatus;
    private String attendanceStatusText;
    private String leaveStatus;
    private String leavePeriod;
    private String leaveReason;
    private String remark;
    private String operatorName;
    private LocalDateTime operateTime;
}
