package com.hkywt.attendance.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.system.entity.SysRole;
import com.hkywt.attendance.module.system.entity.SysUser;
import com.hkywt.attendance.module.system.entity.SysUserRole;
import com.hkywt.attendance.module.system.mapper.SysRoleMapper;
import com.hkywt.attendance.module.system.mapper.SysUserMapper;
import com.hkywt.attendance.module.system.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.bootstrap-admin.username:admin}")
    private String username;

    @Value("${security.bootstrap-admin.password:}")
    private String password;

    public BootstrapAdminInitializer(SysUserMapper userMapper,
                                     SysRoleMapper roleMapper,
                                     SysUserRoleMapper userRoleMapper,
                                     PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) return;
        if (password.length() < 10 || password.length() > 72) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD 长度必须为10到72位");
        }
        Long existing = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0));
        if (existing != null && existing > 0) return;

        SysRole adminRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, RoleCode.ADMIN)
                .eq(SysRole::getDeleted, 0)
                .last("limit 1"));
        if (adminRole == null) throw new IllegalStateException("ADMIN角色不存在，请先执行schema.sql");

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRealName("系统管理员");
        user.setUserType(1);
        user.setStatus(1);
        userMapper.insert(user);

        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(adminRole.getId());
        userRoleMapper.insert(relation);
        log.info("Bootstrap administrator created, username={}", username);
    }
}
