package com.hkywt.attendance.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MemberCreateRequest {
    @NotBlank(message = "姓名不能为空")
    private String realName;
    @NotBlank(message = "账号不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 10, max = 72, message = "密码长度必须为10到72位")
    private String password;
    private String phone;
    private Integer status;
    @NotEmpty(message = "负责班级不能为空")
    private List<Long> classIds;
}
