package com.hkywt.attendance.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 10, max = 72, message = "密码长度必须为10到72位")
    private String newPassword;
}
