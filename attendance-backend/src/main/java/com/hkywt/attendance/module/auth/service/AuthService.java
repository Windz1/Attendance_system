package com.hkywt.attendance.module.auth.service;

import com.hkywt.attendance.module.auth.dto.LoginRequest;
import com.hkywt.attendance.module.auth.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginRequest request, String clientAddress);
}
