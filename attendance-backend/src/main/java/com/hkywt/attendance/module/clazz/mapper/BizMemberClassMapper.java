package com.hkywt.attendance.module.clazz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkywt.attendance.module.clazz.entity.BizMemberClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BizMemberClassMapper extends BaseMapper<BizMemberClass> {
    @Select("SELECT class_id FROM biz_member_class WHERE member_user_id = #{memberUserId} AND deleted = 0 AND status = 1")
    List<Long> findClassIdsByMemberUserId(Long memberUserId);

    @Update("""
            UPDATE biz_member_class
            SET deleted = 0, status = 1, update_by = #{updateBy}, update_time = NOW()
            WHERE member_user_id = #{memberUserId} AND class_id = #{classId}
            """)
    int reactivateMemberClass(Long memberUserId, Long classId, Long updateBy);
}
