package com.hkywt.attendance.module.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Integer userType;
    private Set<String> roles;
    private String homePath;
}
