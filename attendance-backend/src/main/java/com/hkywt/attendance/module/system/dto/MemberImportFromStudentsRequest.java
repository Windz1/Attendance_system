package com.hkywt.attendance.module.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MemberImportFromStudentsRequest {
    @NotEmpty(message = "请选择至少1名学生")
    private List<Long> studentIds;
}

