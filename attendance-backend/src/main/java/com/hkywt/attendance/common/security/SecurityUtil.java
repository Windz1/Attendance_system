package com.hkywt.attendance.common.security;

import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    public static SecurityUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser user)) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().getUserId();
    }

    public static boolean hasRole(String roleCode) {
        SecurityUser user = currentUser();
        if (RoleCode.ADMIN.equals(roleCode) && Integer.valueOf(1).equals(user.getUserType())) {
            return true;
        }
        return user.getRoles() != null && user.getRoles().contains(roleCode);
    }
}
