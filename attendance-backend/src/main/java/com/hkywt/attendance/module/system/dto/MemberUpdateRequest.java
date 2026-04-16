package com.hkywt.attendance.module.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MemberUpdateRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    private String realName;
    private String phone;
    private Integer status;
    private List<Long> classIds;
}
