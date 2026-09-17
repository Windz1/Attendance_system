package com.hkywt.attendance.module.task.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class TaskSubmitRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    @NotEmpty(message = "班级照片不能为空")
    @Size(max = 9, message = "班级照片最多上传9张")
    private List<String> classPhotoUrls;
    @NotEmpty(message = "学生记录不能为空")
    @Valid
    private List<StudentMark> records;

    @Data
    public static class StudentMark {
        @NotNull(message = "学生ID不能为空")
        private Long studentId;
        @NotNull(message = "考勤状态不能为空")
        @Min(value = 1, message = "考勤状态无效")
        @Max(value = 4, message = "考勤状态无效")
        private Integer attendanceStatus;
        @Size(max = 255, message = "备注不能超过255字")
        private String remark;
    }
}
