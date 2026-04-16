package com.hkywt.attendance.module.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class PlanSaveRequest {
    private Long id;
    @NotBlank(message = "计划名称不能为空")
    private String planName;
    @NotNull(message = "计划类型不能为空")
    private Integer planType;
    private Integer status;
    private String timezone;
    private Integer dateMode;
    private String weekdays;
    @NotNull(message = "开始时间不能为空")
    private LocalTime windowStartTime;
    @NotNull(message = "结束时间不能为空")
    private LocalTime windowEndTime;
    @NotNull(message = "生效开始日期不能为空")
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private Integer allowMemberModifyAfterSubmit;
    private String remark;
    @NotEmpty(message = "适用班级不能为空")
    private List<Long> classIds;
}
