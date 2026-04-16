package com.hkywt.attendance.module.task.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TaskSubmitRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    @NotEmpty(message = "学生记录不能为空")
    private List<StudentMark> records;

    @Data
    public static class StudentMark {
        @NotNull(message = "学生ID不能为空")
        private Long studentId;
        @NotNull(message = "考勤状态不能为空")
        private Integer attendanceStatus;
        private String remark;
    }
}
