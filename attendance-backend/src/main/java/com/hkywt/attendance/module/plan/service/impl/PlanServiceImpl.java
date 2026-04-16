package com.hkywt.attendance.module.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.common.security.SecurityUtil;
import com.hkywt.attendance.module.plan.dto.PlanSaveRequest;
import com.hkywt.attendance.module.plan.entity.BizAttendancePlan;
import com.hkywt.attendance.module.plan.entity.BizAttendancePlanClass;
import com.hkywt.attendance.module.plan.mapper.BizAttendancePlanClassMapper;
import com.hkywt.attendance.module.plan.mapper.BizAttendancePlanMapper;
import com.hkywt.attendance.module.plan.service.PlanService;
import com.hkywt.attendance.module.plan.vo.PlanDetailVO;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.task.mapper.BizAttendanceTaskMapper;
import com.hkywt.attendance.module.task.schedule.AttendanceTaskScheduler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PlanServiceImpl implements PlanService {

    private final BizAttendancePlanMapper planMapper;
    private final BizAttendancePlanClassMapper planClassMapper;
    private final BizAttendanceTaskMapper taskMapper;
    private final AttendanceTaskScheduler attendanceTaskScheduler;

    public PlanServiceImpl(BizAttendancePlanMapper planMapper,
                           BizAttendancePlanClassMapper planClassMapper,
                           BizAttendanceTaskMapper taskMapper,
                           AttendanceTaskScheduler attendanceTaskScheduler) {
        this.planMapper = planMapper;
        this.planClassMapper = planClassMapper;
        this.taskMapper = taskMapper;
        this.attendanceTaskScheduler = attendanceTaskScheduler;
    }

    private void checkAdmin() {
        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) throw new BizException(403, "无权限操作");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PlanSaveRequest request) {
        checkAdmin();
        BizAttendancePlan plan = new BizAttendancePlan();
        BeanUtils.copyProperties(request, plan);
        if (request.getId() == null) {
            plan.setCreateBy(SecurityUtil.currentUserId());
            planMapper.insert(plan);
        } else {
            plan.setId(request.getId());
            plan.setUpdateBy(SecurityUtil.currentUserId());
            planMapper.updateById(plan);
            // 关联表存在 uk(plan_id,class_id) 唯一键，编辑时必须物理删除旧关联后再重建
            planClassMapper.physicalDeleteByPlanId(request.getId());
        }
        Long planId = request.getId() == null ? plan.getId() : request.getId();
        request.getClassIds().stream().distinct().forEach(classId -> {
            BizAttendancePlanClass pc = new BizAttendancePlanClass();
            pc.setPlanId(planId);
            pc.setClassId(classId);
            planClassMapper.insert(pc);
        });

        // 计划编辑后清理今天起未提交旧任务，按新计划重建，保证部员端与计划配置实时同步
        taskMapper.physicalDeleteUnsubmittedByPlanIdFromDate(planId, LocalDate.now());
        // 新建/编辑计划后立即尝试生成当天任务，避免必须等待定时器
        attendanceTaskScheduler.generateTasksForDate(LocalDate.now());
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        checkAdmin();
        planMapper.update(null, new LambdaUpdateWrapper<BizAttendancePlan>().eq(BizAttendancePlan::getId, id).set(BizAttendancePlan::getStatus, status));
    }

    @Override
    public void delete(Long id) {
        checkAdmin();
        planMapper.update(null, new LambdaUpdateWrapper<BizAttendancePlan>().eq(BizAttendancePlan::getId, id).set(BizAttendancePlan::getDeleted, 1));
    }

    @Override
    public List<BizAttendancePlan> list() {
        return planMapper.selectList(new LambdaQueryWrapper<BizAttendancePlan>().eq(BizAttendancePlan::getDeleted, 0).orderByDesc(BizAttendancePlan::getId));
    }

    @Override
    public PlanDetailVO getDetail(Long id) {
        BizAttendancePlan plan = planMapper.selectOne(new LambdaQueryWrapper<BizAttendancePlan>()
                .eq(BizAttendancePlan::getId, id)
                .eq(BizAttendancePlan::getDeleted, 0)
                .last("limit 1"));
        if (plan == null) throw new BizException("计划不存在");
        PlanDetailVO vo = new PlanDetailVO();
        vo.setPlan(plan);
        vo.setClassIds(planClassMapper.findClassIdsByPlanId(id));
        return vo;
    }
}
