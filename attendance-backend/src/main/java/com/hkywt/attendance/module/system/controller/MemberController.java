package com.hkywt.attendance.module.system.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.system.dto.MemberCreateRequest;
import com.hkywt.attendance.module.system.dto.MemberImportFromStudentsRequest;
import com.hkywt.attendance.module.system.dto.MemberUpdateRequest;
import com.hkywt.attendance.module.system.entity.SysUser;
import com.hkywt.attendance.module.system.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody MemberCreateRequest request) {
        memberService.createMember(request);
        return ApiResult.success();
    }

    @PostMapping("/import-from-students")
    public ApiResult<String> importFromStudents(@Valid @RequestBody MemberImportFromStudentsRequest request) {
        return ApiResult.success(memberService.importFromStudents(request.getStudentIds()));
    }

    @PutMapping
    public ApiResult<Void> update(@Valid @RequestBody MemberUpdateRequest request) {
        memberService.updateMember(request);
        return ApiResult.success();
    }

    @PutMapping("/{userId}/reset-password")
    public ApiResult<Void> resetPassword(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        memberService.resetPassword(userId, body.getOrDefault("newPassword", "123456"));
        return ApiResult.success();
    }

    @DeleteMapping("/{userId}")
    public ApiResult<Void> delete(@PathVariable Long userId) {
        memberService.deleteMember(userId);
        return ApiResult.success();
    }

    @GetMapping
    public ApiResult<List<SysUser>> list(@RequestParam(required = false) String keyword) {
        return ApiResult.success(memberService.listMembers(keyword));
    }

    @GetMapping("/{userId}/class-ids")
    public ApiResult<List<Long>> classIds(@PathVariable Long userId) {
        return ApiResult.success(memberService.memberClassIds(userId));
    }
}
