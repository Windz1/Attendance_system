package com.hkywt.attendance.module.student.service;

import com.hkywt.attendance.module.student.dto.StudentSaveRequest;
import com.hkywt.attendance.module.student.entity.BizStudent;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {
    void save(StudentSaveRequest request);
    void delete(Long studentId);
    List<BizStudent> list(String keyword, Long classId);
    String importXlsx(MultipartFile file, Integer importMode);
}
