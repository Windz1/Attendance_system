package com.hkywt.attendance.module.system.service;

import com.hkywt.attendance.module.system.dto.MemberCreateRequest;
import com.hkywt.attendance.module.system.dto.MemberUpdateRequest;
import com.hkywt.attendance.module.system.entity.SysUser;

import java.util.List;

public interface MemberService {
    void createMember(MemberCreateRequest request);
    String importFromStudents(List<Long> studentIds);
    void updateMember(MemberUpdateRequest request);
    void resetPassword(Long userId, String newPassword);
    void deleteMember(Long userId);
    List<SysUser> listMembers(String keyword);
    List<Long> memberClassIds(Long userId);
}
