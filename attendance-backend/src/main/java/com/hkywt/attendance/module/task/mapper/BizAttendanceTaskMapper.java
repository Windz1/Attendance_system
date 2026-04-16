package com.hkywt.attendance.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkywt.attendance.module.task.entity.BizAttendanceTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface BizAttendanceTaskMapper extends BaseMapper<BizAttendanceTask> {
    @Delete("""
            DELETE FROM biz_attendance_task
            WHERE plan_id = #{planId}
              AND task_date >= #{fromDate}
              AND task_status IN (1,2,4)
            """)
    int physicalDeleteUnsubmittedByPlanIdFromDate(@Param("planId") Long planId, @Param("fromDate") LocalDate fromDate);
}
