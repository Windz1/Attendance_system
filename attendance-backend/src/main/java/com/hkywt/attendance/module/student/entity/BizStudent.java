package com.hkywt.attendance.module.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_student")
public class BizStudent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String studentName;
    private Integer gender;
    private Long classId;
    private String major;
    private String grade;
    private String college;
    private String phone;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
