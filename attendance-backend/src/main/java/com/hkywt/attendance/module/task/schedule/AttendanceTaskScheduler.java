package com.hkywt.attendance.module.task.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hkywt.attendance.module.plan.entity.BizAttendancePlan;
import com.hkywt.attendance.module.plan.mapper.BizAttendancePlanClassMapper;
import com.hkywt.attendance.module.plan.mapper.BizAttendancePlanMapper;
import com.hkywt.attendance.module.record.mapper.BizAttendanceRecordMapper;
import com.hkywt.attendance.module.student.entity.BizStudent;
import com.hkywt.attendance.module.student.mapper.BizStudentMapper;
import com.hkywt.attendance.module.task.entity.BizAttendanceTask;
import com.hkywt.attendance.module.task.mapper.BizAttendanceTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AttendanceTaskScheduler {

    private final BizAttendancePlanMapper planMapper;
    private final BizAttendancePlanClassMapper planClassMapper;
    private final BizAttendanceTaskMapper taskMapper;
    private final BizStudentMapper studentMapper;
    private final BizAttendanceRecordMapper recordMapper;

    public AttendanceTaskScheduler(BizAttendancePlanMapper planMapper,
                                   BizAttendancePlanClassMapper planClassMapper,
                                   BizAttendanceTaskMapper taskMapper,
                                   BizStudentMapper studentMapper,
                                   BizAttendanceRecordMapper recordMapper) {
        this.planMapper = planMapper;
        this.planClassMapper = planClassMapper;
        this.taskMapper = taskMapper;
        this.studentMapper = studentMapper;
        this.recordMapper = recordMapper;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void generateTodayTasks() {
        generateTasksForDate(LocalDate.now());
    }

    public void generateTasksForDate(LocalDate today) {
        List<BizAttendancePlan> plans = planMapper.selectList(new LambdaQueryWrapper<BizAttendancePlan>()
                .eq(BizAttendancePlan::getDeleted, 0)
                .eq(BizAttendancePlan::getStatus, 1)
                .le(BizAttendancePlan::getEffectiveStartDate, today)
                .and(w -> w.isNull(BizAttendancePlan::getEffectiveEndDate).or().ge(BizAttendancePlan::getEffectiveEndDate, today)));

        int created = 0;
        for (BizAttendancePlan plan : plans) {
            if (!weekdayMatched(plan.getWeekdays(), today.getDayOfWeek())) continue;
            List<Long> classIds = planClassMapper.findClassIdsByPlanId(plan.getId());
            for (Long classId : classIds) {
                Long studentCount = studentMapper.selectCount(new LambdaQueryWrapper<BizStudent>()
                        .eq(BizStudent::getDeleted, 0)
                        .eq(BizStudent::getClassId, classId));
                if (studentCount == null || studentCount == 0) {
                    continue;
                }
                Long exists = taskMapper.selectCount(new LambdaQueryWrapper<BizAttendanceTask>()
                        .eq(BizAttendanceTask::getDeleted, 0)
                        .eq(BizAttendanceTask::getPlanId, plan.getId())
                        .eq(BizAttendanceTask::getClassId, classId)
                        .eq(BizAttendanceTask::getTaskDate, today));
                if (exists != null && exists > 0) continue;

                ZoneId zoneId = ZoneId.of(plan.getTimezone() == null || plan.getTimezone().isBlank() ? "Asia/Shanghai" : plan.getTimezone());
                LocalDateTime start = ZonedDateTime.of(today, plan.getWindowStartTime(), zoneId).toLocalDateTime();
                LocalDateTime end = ZonedDateTime.of(today, plan.getWindowEndTime(), zoneId).toLocalDateTime();

                BizAttendanceTask task = new BizAttendanceTask();
                task.setTaskNo("TASK" + System.currentTimeMillis() + classId);
                task.setPlanId(plan.getId());
                task.setClassId(classId);
                task.setTaskDate(today);
                task.setWindowStartAt(start);
                task.setWindowEndAt(end);
                task.setTaskStatus(LocalDateTime.now().isAfter(end) ? 4 : 1);
                task.setAllowModify(plan.getAllowMemberModifyAfterSubmit() == null ? 0 : plan.getAllowMemberModifyAfterSubmit());
                taskMapper.insert(task);
                created++;
            }
        }
        if (created > 0) log.info("Generated attendance tasks count={}", created);
    }

    @Scheduled(cron = "0 */2 * * * ?")
    public void expireTimeoutTasks() {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.update(null, new LambdaUpdateWrapper<BizAttendanceTask>()
                .eq(BizAttendanceTask::getDeleted, 0)
                .in(BizAttendanceTask::getTaskStatus, List.of(1, 2))
                .lt(BizAttendanceTask::getWindowEndAt, now)
                .set(BizAttendanceTask::getTaskStatus, 4));
    }

    /**
     * 每天凌晨清理一个月前的考勤记录，避免表数据无限增长
     */
    @Scheduled(cron = "0 10 3 * * ?")
    public void cleanupOldRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(1);
        int deleted = recordMapper.physicalDeleteBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleanup old attendance records count={}, cutoff={}", deleted, cutoff);
        }
    }

    private boolean weekdayMatched(String weekdays, DayOfWeek dayOfWeek) {
        if (weekdays == null || weekdays.isBlank()) return true;
        Set<Integer> set = java.util.Arrays.stream(weekdays.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        return set.contains(dayOfWeek.getValue()); // Monday=1 ... Sunday=7
    }
}
