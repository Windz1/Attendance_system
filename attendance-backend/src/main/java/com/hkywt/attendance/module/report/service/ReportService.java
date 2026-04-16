package com.hkywt.attendance.module.report.service;

import com.hkywt.attendance.module.report.vo.ClassStatusStatVO;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    List<ClassStatusStatVO> classStatusStat(LocalDate startDate, LocalDate endDate, Long classId);
}
