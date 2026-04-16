package com.hkywt.attendance.module.record.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class RecordExportRow {
    @ExcelProperty("任务编号")
    private Long taskId;
    @ExcelProperty("计划名称")
    private String planName;
    @ExcelProperty("班级")
    private String className;
    @ExcelProperty("窗口开始")
    private String windowStartAt;
    @ExcelProperty("窗口结束")
    private String windowEndAt;
    @ExcelProperty("提交时间")
    private String submittedAt;
    @ExcelProperty("学生姓名")
    private String studentName;
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("考勤状态")
    private String attendanceStatus;
    @ExcelProperty("请假状态")
    private String leaveStatus;
    @ExcelProperty("点名员")
    private String operatorName;
}
