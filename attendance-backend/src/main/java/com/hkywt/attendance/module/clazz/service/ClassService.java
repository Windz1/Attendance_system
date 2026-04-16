package com.hkywt.attendance.module.clazz.service;

import com.hkywt.attendance.module.clazz.dto.ClassSaveRequest;
import com.hkywt.attendance.module.clazz.entity.BizClass;

import java.util.List;

public interface ClassService {
    List<BizClass> listAll(String keyword);
    List<BizClass> listMyClasses();
    void save(ClassSaveRequest request);
    void delete(Long id);
}
