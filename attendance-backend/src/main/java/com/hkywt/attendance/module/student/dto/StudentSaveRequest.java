package com.hkywt.attendance.module.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentSaveRequest {
    private Long id;
    @NotBlank(message = "学号不能为空")
    private String studentNo;
    @NotBlank(message = "姓名不能为空")
    private String studentName;
    private Integer gender;
    @NotNull(message = "班级不能为空")
    private Long classId;
    private String major;
    private String grade;
    private String college;
    private String phone;
    private Integer status;
}
