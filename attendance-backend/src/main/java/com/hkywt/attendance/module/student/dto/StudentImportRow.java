package com.hkywt.attendance.module.student.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentImportRow {
    @ExcelProperty(index = 0)
    private String studentNo;
    @ExcelProperty(index = 1)
    private String studentName;
    @ExcelProperty(index = 2)
    private String gender;
    @ExcelProperty(index = 3)
    private String grade;
    @ExcelProperty(index = 4)
    private String college;
    @ExcelProperty(index = 5)
    private String major;
    @ExcelProperty(index = 6)
    private String className;
    @ExcelProperty(index = 7)
    private String phone;
    @ExcelProperty(index = 8)
    private String status;
}
