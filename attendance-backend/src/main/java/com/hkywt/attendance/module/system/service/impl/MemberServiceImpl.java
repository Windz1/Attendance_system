package com.hkywt.attendance.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.common.security.SecurityUtil;
import com.hkywt.attendance.module.clazz.entity.BizMemberClass;
import com.hkywt.attendance.module.clazz.mapper.BizMemberClassMapper;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.student.entity.BizStudent;
import com.hkywt.attendance.module.student.mapper.BizStudentMapper;
import com.hkywt.attendance.module.system.dto.MemberCreateRequest;
import com.hkywt.attendance.module.system.dto.MemberUpdateRequest;
import com.hkywt.attendance.module.system.entity.SysRole;
import com.hkywt.attendance.module.system.entity.SysUser;
import com.hkywt.attendance.module.system.entity.SysUserRole;
import com.hkywt.attendance.module.system.mapper.SysRoleMapper;
import com.hkywt.attendance.module.system.mapper.SysUserMapper;
import com.hkywt.attendance.module.system.mapper.SysUserRoleMapper;
import com.hkywt.attendance.module.system.service.MemberService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MemberServiceImpl implements MemberService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BizMemberClassMapper memberClassMapper;
    private final BizStudentMapper studentMapper;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper, BizMemberClassMapper memberClassMapper, BizStudentMapper studentMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.memberClassMapper = memberClassMapper;
        this.studentMapper = studentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    private void checkAdmin() {
        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) {
            throw new BizException(403, "无权限操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMember(MemberCreateRequest request) {
        checkAdmin();
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()).eq(SysUser::getDeleted, 0));
        if (exists > 0) {
            throw new BizException("账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setUserType(2);
        user.setCreateBy(SecurityUtil.currentUserId());
        userMapper.insert(user);

        SysRole memberRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, RoleCode.MEMBER).eq(SysRole::getDeleted, 0).last("limit 1"));
        if (memberRole == null) {
            throw new BizException("MEMBER角色不存在");
        }
        SysUserRole ur = new SysUserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(memberRole.getId());
        userRoleMapper.insert(ur);
        syncMemberClasses(user.getId(), request.getClassIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importFromStudents(List<Long> studentIds) {
        checkAdmin();
        Set<Long> targetIds = new LinkedHashSet<>(studentIds);
        if (targetIds.isEmpty()) {
            throw new BizException("请选择至少1名学生");
        }

        SysRole memberRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, RoleCode.MEMBER)
                .eq(SysRole::getDeleted, 0)
                .last("limit 1"));
        if (memberRole == null) {
            throw new BizException("MEMBER角色不存在");
        }

        List<BizStudent> students = studentMapper.selectList(new LambdaQueryWrapper<BizStudent>()
                .eq(BizStudent::getDeleted, 0)
                .in(BizStudent::getId, targetIds));
        Map<Long, BizStudent> studentMap = students.stream().collect(Collectors.toMap(BizStudent::getId, Function.identity()));

        int created = 0;
        int recovered = 0;
        int skippedExists = 0;
        int skippedInvalid = 0;
        Long operator = SecurityUtil.currentUserId();

        for (Long studentId : targetIds) {
            BizStudent student = studentMap.get(studentId);
            if (student == null || student.getStudentNo() == null || student.getStudentNo().isBlank() || student.getClassId() == null) {
                skippedInvalid++;
                continue;
            }

            SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, student.getStudentNo())
                    .last("limit 1"));
            if (existing != null && existing.getDeleted() != null && existing.getDeleted() == 0) {
                skippedExists++;
                continue;
            }

            Long userId;
            if (existing != null) {
                existing.setDeleted(0);
                existing.setStatus(1);
                existing.setUserType(2);
                existing.setRealName(student.getStudentName());
                existing.setPhone(student.getPhone());
                existing.setPasswordHash(passwordEncoder.encode(student.getStudentNo()));
                existing.setUpdateBy(operator);
                userMapper.updateById(existing);
                userId = existing.getId();
                recovered++;
            } else {
                SysUser user = new SysUser();
                user.setUsername(student.getStudentNo());
                user.setPasswordHash(passwordEncoder.encode(student.getStudentNo()));
                user.setRealName(student.getStudentName());
                user.setPhone(student.getPhone());
                user.setStatus(1);
                user.setUserType(2);
                user.setCreateBy(operator);
                userMapper.insert(user);
                userId = user.getId();
                created++;
            }

            ensureMemberRole(userId, memberRole.getId());
            syncMemberClasses(userId, List.of(student.getClassId()));
        }
        return "导入完成：新增" + created + "人，恢复" + recovered + "人，已存在跳过" + skippedExists + "人，无效数据跳过" + skippedInvalid + "人。默认账号=学号，默认密码=学号。";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMember(MemberUpdateRequest request) {
        checkAdmin();
        SysUser user = userMapper.selectById(request.getUserId());
        if (user == null || user.getDeleted() == 1) {
            throw new BizException("用户不存在");
        }
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        user.setUpdateBy(SecurityUtil.currentUserId());
        userMapper.updateById(user);

        if (request.getClassIds() != null) {
            syncMemberClasses(request.getUserId(), request.getClassIds());
        }
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        checkAdmin();
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .eq(SysUser::getDeleted, 0)
                .set(SysUser::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(SysUser::getUpdateBy, SecurityUtil.currentUserId()));
    }

    @Override
    public void deleteMember(Long userId) {
        checkAdmin();
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>().eq(SysUser::getId, userId).set(SysUser::getDeleted, 1));
    }

    @Override
    public List<SysUser> listMembers(String keyword) {
        checkAdmin();
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, 2)
                .eq(SysUser::getDeleted, 0)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(SysUser::getRealName, keyword).or().like(SysUser::getUsername, keyword))
                .orderByDesc(SysUser::getId));
    }

    @Override
    public List<Long> memberClassIds(Long userId) {
        checkAdmin();
        return memberClassMapper.findClassIdsByMemberUserId(userId);
    }

    private void syncMemberClasses(Long memberUserId, List<Long> classIds) {
        Set<Long> target = new LinkedHashSet<>(classIds);
        Long operator = SecurityUtil.currentUserId();

        for (Long classId : target) {
            int affected = memberClassMapper.reactivateMemberClass(memberUserId, classId, operator);
            if (affected == 0) {
                BizMemberClass mc = new BizMemberClass();
                mc.setMemberUserId(memberUserId);
                mc.setClassId(classId);
                mc.setStatus(1);
                mc.setCreateBy(operator);
                memberClassMapper.insert(mc);
            }
        }

        LambdaUpdateWrapper<BizMemberClass> wrapper = new LambdaUpdateWrapper<BizMemberClass>()
                .eq(BizMemberClass::getMemberUserId, memberUserId)
                .eq(BizMemberClass::getDeleted, 0)
                .set(BizMemberClass::getDeleted, 1)
                .set(BizMemberClass::getStatus, 0)
                .set(BizMemberClass::getUpdateBy, operator);
        if (!target.isEmpty()) {
            wrapper.notIn(BizMemberClass::getClassId, target);
        }
        memberClassMapper.update(null, wrapper);
    }

    private void ensureMemberRole(Long userId, Long memberRoleId) {
        SysUserRole exists = userRoleMapper.selectOne(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, memberRoleId)
                .last("limit 1"));
        if (exists == null) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(memberRoleId);
            userRoleMapper.insert(ur);
            return;
        }
        if (exists.getDeleted() != null && exists.getDeleted() == 1) {
            exists.setDeleted(0);
            userRoleMapper.updateById(exists);
        }
    }
}
