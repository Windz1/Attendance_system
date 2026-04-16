package com.hkywt.attendance.module.task.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDetailVO {
    private Long taskId;
    private String taskNo;
    private Long classId;
    private Long planId;
    private Integer taskStatus;
    private Integer allowModify;
    private LocalDate taskDate;
    private LocalDateTime windowStartAt;
    private LocalDateTime windowEndAt;
    private List<StudentMarkVO> students;

    @Data
    public static class StudentMarkVO {
        private Long studentId;
        private String studentNo;
        private String studentName;
        private Integer attendanceStatus;
        private String remark;
    }
}
