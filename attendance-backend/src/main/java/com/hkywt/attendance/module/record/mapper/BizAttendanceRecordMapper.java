package com.hkywt.attendance.module.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hkywt.attendance.module.record.entity.BizAttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface BizAttendanceRecordMapper extends BaseMapper<BizAttendanceRecord> {
    @Delete("DELETE FROM biz_attendance_record WHERE operate_time < #{cutoff}")
    int physicalDeleteBefore(@Param("cutoff") LocalDateTime cutoff);
}
