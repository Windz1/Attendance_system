package com.hkywt.attendance.module.clazz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_class")
public class BizClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String classCode;
    private String className;
    private String major;
    private String grade;
    private String college;
    private Integer status;
    private String remark;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
