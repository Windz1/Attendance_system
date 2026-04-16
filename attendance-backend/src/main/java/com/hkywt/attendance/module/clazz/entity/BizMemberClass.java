package com.hkywt.attendance.module.clazz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_member_class")
public class BizMemberClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberUserId;
    private Long classId;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
