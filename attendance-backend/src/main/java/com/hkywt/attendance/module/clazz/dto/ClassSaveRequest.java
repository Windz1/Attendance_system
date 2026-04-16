package com.hkywt.attendance.module.clazz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassSaveRequest {
    private Long id;
    @NotBlank(message = "班级编码不能为空")
    private String classCode;
    @NotBlank(message = "班级名称不能为空")
    private String className;
    private String major;
    private String grade;
    private String college;
    private Integer status;
    private String remark;
}
