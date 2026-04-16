package com.hkywt.attendance.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.common.util.JwtUtil;
import com.hkywt.attendance.module.auth.dto.LoginRequest;
import com.hkywt.attendance.module.auth.service.AuthService;
import com.hkywt.attendance.module.auth.vo.LoginVO;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.system.entity.SysUser;
import com.hkywt.attendance.module.system.mapper.SysRoleMapper;
import com.hkywt.attendance.module.system.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginVO login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getDeleted, 0)
                .last("limit 1"));
        if (user == null || user.getStatus() == 0 || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(401, "账号或密码错误");
        }

        List<String> roleList = roleMapper.findRoleCodesByUserId(user.getId());
        Set<String> roles = roleList.stream().collect(Collectors.toSet());
        String token = jwtUtil.generateToken(user.getUsername(), new HashMap<>() {{
            put("userId", user.getId());
            put("realName", user.getRealName());
            put("userType", user.getUserType());
            put("roles", roleList);
        }});

        boolean isAdmin = roles.contains(RoleCode.ADMIN) || Integer.valueOf(1).equals(user.getUserType());
        String homePath = isAdmin ? "/dashboard" : "/m/tasks";
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(user.getUserType())
                .roles(roles)
                .homePath(homePath)
                .build();
    }
}
