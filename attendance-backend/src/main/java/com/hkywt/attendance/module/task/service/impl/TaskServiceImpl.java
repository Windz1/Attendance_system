package com.hkywt.attendance.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.common.security.SecurityUtil;
import com.hkywt.attendance.module.clazz.mapper.BizMemberClassMapper;
import com.hkywt.attendance.module.plan.entity.BizAttendancePlan;
import com.hkywt.attendance.module.plan.mapper.BizAttendancePlanClassMapper;
import com.hkywt.attendance.module.plan.mapper.BizAttendancePlanMapper;
import com.hkywt.attendance.module.record.entity.BizAttendanceRecord;
import com.hkywt.attendance.module.record.mapper.BizAttendanceRecordMapper;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.student.entity.BizStudent;
import com.hkywt.attendance.module.student.mapper.BizStudentMapper;
import com.hkywt.attendance.module.task.dto.TaskSubmitRequest;
import com.hkywt.attendance.module.task.entity.BizAttendanceTask;
import com.hkywt.attendance.module.task.mapper.BizAttendanceTaskMapper;
import com.hkywt.attendance.module.task.service.TaskService;
import com.hkywt.attendance.module.task.vo.TaskDetailVO;
import com.hkywt.attendance.module.task.vo.TaskListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private final BizAttendanceTaskMapper taskMapper;
    private final BizAttendancePlanMapper planMapper;
    private final BizAttendancePlanClassMapper planClassMapper;
    private final BizMemberClassMapper memberClassMapper;
    private final BizAttendanceRecordMapper recordMapper;
    private final BizStudentMapper studentMapper;

    public TaskServiceImpl(BizAttendanceTaskMapper taskMapper,
                           BizAttendancePlanMapper planMapper,
                           BizAttendancePlanClassMapper planClassMapper,
                           BizMemberClassMapper memberClassMapper,
                           BizAttendanceRecordMapper recordMapper,
                           BizStudentMapper studentMapper) {
        this.taskMapper = taskMapper;
        this.planMapper = planMapper;
        this.planClassMapper = planClassMapper;
        this.memberClassMapper = memberClassMapper;
        this.recordMapper = recordMapper;
        this.studentMapper = studentMapper;
    }

    @Override
    public List<TaskListVO> listMyPendingTasks() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<BizAttendanceTask> wrapper = new LambdaQueryWrapper<BizAttendanceTask>()
                .eq(BizAttendanceTask::getDeleted, 0)
                .eq(BizAttendanceTask::getTaskDate, LocalDate.now())
                .le(BizAttendanceTask::getWindowStartAt, now)
                .ge(BizAttendanceTask::getWindowEndAt, now);

        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) {
            List<Long> myClassIds = memberClassMapper.findClassIdsByMemberUserId(SecurityUtil.currentUserId());
            if (myClassIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(BizAttendanceTask::getClassId, myClassIds);
        }

        List<BizAttendanceTask> tasks = taskMapper.selectList(wrapper
                .in(BizAttendanceTask::getTaskStatus, List.of(1, 2, 3))
                .orderByAsc(BizAttendanceTask::getWindowStartAt));
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<Long, BizAttendancePlan> planMap = planMapper.selectBatchIds(tasks.stream().map(BizAttendanceTask::getPlanId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(BizAttendancePlan::getId, p -> p));

        Map<Long, Set<Long>> planClassMap = new HashMap<>();
        for (Long planId : planMap.keySet()) {
            planClassMap.put(planId, Set.copyOf(planClassMapper.findClassIdsByPlanId(planId)));
        }

        return tasks.stream()
                .filter(task -> matchCurrentPlanRule(task, planMap.get(task.getPlanId())))
                .filter(task -> planClassMap.getOrDefault(task.getPlanId(), Set.of()).contains(task.getClassId()))
                .map(task -> {
            BizAttendancePlan plan = planMap.get(task.getPlanId());
            TaskListVO vo = new TaskListVO();
            vo.setId(task.getId());
            vo.setPlanId(task.getPlanId());
            vo.setPlanName(plan == null ? "" : plan.getPlanName());
            vo.setSessionLabel(resolveSessionLabel(plan, task.getWindowStartAt()));
            vo.setClassId(task.getClassId());
            vo.setTaskDate(task.getTaskDate());
            vo.setTaskStatus(task.getTaskStatus());
            return vo;
        }).toList();
    }

    @Override
    public TaskDetailVO getTaskDetail(Long taskId) {
        BizAttendanceTask task = taskMapper.selectById(taskId);
        if (task == null || task.getDeleted() == 1) throw new BizException("任务不存在");
        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) {
            List<Long> myClassIds = memberClassMapper.findClassIdsByMemberUserId(SecurityUtil.currentUserId());
            if (!myClassIds.contains(task.getClassId())) throw new BizException(403, "无权限查看该任务");
        }

        List<BizStudent> students = studentMapper.selectList(new LambdaQueryWrapper<BizStudent>()
                .eq(BizStudent::getDeleted, 0)
                .eq(BizStudent::getClassId, task.getClassId())
                .orderByAsc(BizStudent::getStudentNo));
        List<BizAttendanceRecord> records = recordMapper.selectList(new LambdaQueryWrapper<BizAttendanceRecord>()
                .eq(BizAttendanceRecord::getDeleted, 0)
                .eq(BizAttendanceRecord::getTaskId, taskId));
        Map<Long, BizAttendanceRecord> recordMap = new HashMap<>();
        for (BizAttendanceRecord r : records) recordMap.put(r.getStudentId(), r);

        TaskDetailVO vo = new TaskDetailVO();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setClassId(task.getClassId());
        vo.setPlanId(task.getPlanId());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setAllowModify(task.getAllowModify());
        vo.setTaskDate(task.getTaskDate());
        vo.setWindowStartAt(task.getWindowStartAt());
        vo.setWindowEndAt(task.getWindowEndAt());
        vo.setStudents(students.stream().map(s -> {
            TaskDetailVO.StudentMarkVO row = new TaskDetailVO.StudentMarkVO();
            row.setStudentId(s.getId());
            row.setStudentNo(s.getStudentNo());
            row.setStudentName(s.getStudentName());
            BizAttendanceRecord rec = recordMap.get(s.getId());
            row.setAttendanceStatus(rec == null ? 1 : rec.getAttendanceStatus());
            row.setRemark(rec == null ? "" : rec.getRemark());
            return row;
        }).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(TaskSubmitRequest request) {
        BizAttendanceTask task = taskMapper.selectById(request.getTaskId());
        if (task == null || task.getDeleted() == 1) throw new BizException("任务不存在");

        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) {
            List<Long> myClassIds = memberClassMapper.findClassIdsByMemberUserId(SecurityUtil.currentUserId());
            if (!myClassIds.contains(task.getClassId())) {
                throw new BizException(403, "无权限提交该班级任务");
            }
        }

        Set<Long> validStudentIds = studentMapper.selectList(new LambdaQueryWrapper<BizStudent>()
                        .eq(BizStudent::getClassId, task.getClassId())
                        .eq(BizStudent::getDeleted, 0))
                .stream().map(BizStudent::getId).collect(Collectors.toSet());

        for (TaskSubmitRequest.StudentMark row : request.getRecords()) {
            if (!validStudentIds.contains(row.getStudentId())) {
                throw new BizException("存在不属于该班级的学生ID:" + row.getStudentId());
            }
            BizAttendanceRecord record = recordMapper.selectOne(new LambdaQueryWrapper<BizAttendanceRecord>()
                    .eq(BizAttendanceRecord::getTaskId, task.getId())
                    .eq(BizAttendanceRecord::getStudentId, row.getStudentId())
                    .eq(BizAttendanceRecord::getDeleted, 0)
                    .last("limit 1"));

            if (record == null) {
                record = new BizAttendanceRecord();
                record.setTaskId(task.getId());
                record.setPlanId(task.getPlanId());
                record.setClassId(task.getClassId());
                record.setStudentId(row.getStudentId());
                record.setAttendanceStatus(row.getAttendanceStatus());
                record.setIsDefaultMarked(row.getAttendanceStatus() == 1 ? 1 : 0);
                record.setRemark(row.getRemark());
                record.setOperatorUserId(SecurityUtil.currentUserId());
                record.setOperateTime(LocalDateTime.now());
                recordMapper.insert(record);
            } else {
                record.setAttendanceStatus(row.getAttendanceStatus());
                record.setIsDefaultMarked(row.getAttendanceStatus() == 1 ? 1 : 0);
                record.setRemark(row.getRemark());
                record.setOperatorUserId(SecurityUtil.currentUserId());
                record.setOperateTime(LocalDateTime.now());
                recordMapper.updateById(record);
            }
        }

        task.setTaskStatus(3);
        task.setSubmitterUserId(SecurityUtil.currentUserId());
        task.setSubmittedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private String resolveSessionLabel(BizAttendancePlan plan, LocalDateTime windowStartAt) {
        Integer planType = plan == null ? null : plan.getPlanType();
        if (planType != null) {
            if (planType == 1) return "早自习";
            if (planType == 2) return "晚自习";
            if (planType == 3) return "其他";
        }
        String planName = plan == null ? "" : plan.getPlanName();
        String name = planName == null ? "" : planName;
        if (name.contains("早")) return "早自习";
        if (name.contains("晚")) return "晚自习";
        int hour = windowStartAt == null ? 0 : windowStartAt.getHour();
        return hour < 12 ? "早自习" : "晚自习";
    }

    private boolean matchCurrentPlanRule(BizAttendanceTask task, BizAttendancePlan plan) {
        if (task == null || plan == null) return false;
        if (plan.getDeleted() != null && plan.getDeleted() == 1) return false;
        if (plan.getStatus() == null || plan.getStatus() != 1) return false;
        if (task.getTaskDate() == null) return false;
        if (plan.getEffectiveStartDate() != null && task.getTaskDate().isBefore(plan.getEffectiveStartDate())) return false;
        if (plan.getEffectiveEndDate() != null && task.getTaskDate().isAfter(plan.getEffectiveEndDate())) return false;
        return weekdayMatched(plan.getWeekdays(), task.getTaskDate().getDayOfWeek());
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
