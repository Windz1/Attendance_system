package com.hkywt.attendance.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkywt.attendance.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("""
            SELECT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            """)
    List<String> findRoleCodesByUserId(Long userId);
}
