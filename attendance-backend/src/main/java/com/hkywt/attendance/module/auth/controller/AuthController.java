package com.hkywt.attendance.module.auth.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.common.security.SecurityUtil;
import com.hkywt.attendance.module.auth.dto.LoginRequest;
import com.hkywt.attendance.module.auth.service.AuthService;
import com.hkywt.attendance.module.auth.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResult.success(authService.login(request, servletRequest.getRemoteAddr()));
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        var user = SecurityUtil.currentUser();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("userType", user.getUserType());
        data.put("roles", user.getRoles());
        return ApiResult.success(data);
    }
}
