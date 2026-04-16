package com.hkywt.attendance.module.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_student_import_log")
public class BizStudentImportLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String importBatchNo;
    private String fileName;
    private Integer importMode;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String resultMessage;
    private Long operatorUserId;
    private LocalDateTime createTime;
}
