package com.hkywt.attendance.module.plan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkywt.attendance.module.plan.entity.BizAttendancePlanClass;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BizAttendancePlanClassMapper extends BaseMapper<BizAttendancePlanClass> {
    @Select("SELECT class_id FROM biz_attendance_plan_class WHERE plan_id = #{planId} AND deleted = 0")
    List<Long> findClassIdsByPlanId(Long planId);

    @Delete("DELETE FROM biz_attendance_plan_class WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(Long planId);
}
