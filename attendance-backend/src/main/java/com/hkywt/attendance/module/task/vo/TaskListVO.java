package com.hkywt.attendance.module.task.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskListVO {
    private Long id;
    private Long planId;
    private String planName;
    private String sessionLabel;
    private Long classId;
    private LocalDate taskDate;
    private Integer taskStatus;
}

