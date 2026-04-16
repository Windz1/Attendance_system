package com.hkywt.attendance.module.student.controller;

import com.hkywt.attendance.common.model.ApiResult;
import com.hkywt.attendance.module.student.dto.StudentSaveRequest;
import com.hkywt.attendance.module.student.entity.BizStudent;
import com.hkywt.attendance.module.student.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ApiResult<Void> save(@Valid @RequestBody StudentSaveRequest request) {
        studentService.save(request);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResult.success();
    }

    @GetMapping
    public ApiResult<List<BizStudent>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long classId) {
        return ApiResult.success(studentService.list(keyword, classId));
    }

    @PostMapping("/import")
    public ApiResult<String> importXlsx(@RequestParam("file") MultipartFile file,
                                        @RequestParam(defaultValue = "1") Integer importMode) {
        return ApiResult.success(studentService.importXlsx(file, importMode));
    }
}
