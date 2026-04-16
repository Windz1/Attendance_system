package com.hkywt.attendance.module.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClassStatusStatVO {
    private Long classId;
    private Integer attendanceStatus;
    private Long count;
}
