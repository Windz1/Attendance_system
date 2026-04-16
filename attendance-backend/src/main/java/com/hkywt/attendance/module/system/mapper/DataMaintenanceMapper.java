package com.hkywt.attendance.module.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataMaintenanceMapper {

    @Delete("DELETE FROM biz_attendance_record")
    int deleteAllAttendanceRecords();

    @Delete("DELETE FROM biz_attendance_task")
    int deleteAllAttendanceTasks();

    @Delete("DELETE FROM biz_attendance_plan_class")
    int deleteAllAttendancePlanClasses();

    @Delete("DELETE FROM biz_attendance_plan")
    int deleteAllAttendancePlans();

    @Delete("DELETE FROM biz_student_import_log")
    int deleteAllStudentImportLogs();

    @Delete("DELETE FROM biz_member_class")
    int deleteAllMemberClasses();

    @Delete("DELETE FROM biz_student")
    int deleteAllStudents();

    @Delete("DELETE FROM biz_class")
    int deleteAllClasses();
}
