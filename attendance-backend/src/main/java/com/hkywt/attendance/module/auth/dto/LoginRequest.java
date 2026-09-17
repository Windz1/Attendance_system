package com.hkywt.attendance.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "账号不能为空")
    @Size(max = 64, message = "账号长度不能超过64位")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码长度不能超过72位")
    private String password;
}
