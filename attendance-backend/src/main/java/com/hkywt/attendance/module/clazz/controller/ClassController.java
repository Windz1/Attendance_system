package com.hkywt.attendance.module.clazz.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.clazz.dto.ClassSaveRequest;
import com.hkywt.attendance.module.clazz.entity.BizClass;
import com.hkywt.attendance.module.clazz.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public ApiResult<List<BizClass>> list(@RequestParam(required = false) String keyword) {
        return ApiResult.success(classService.listAll(keyword));
    }

    @GetMapping("/my")
    public ApiResult<List<BizClass>> myClasses() {
        return ApiResult.success(classService.listMyClasses());
    }

    @PostMapping
    public ApiResult<Void> save(@Valid @RequestBody ClassSaveRequest request) {
        classService.save(request);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return ApiResult.success();
    }
}
