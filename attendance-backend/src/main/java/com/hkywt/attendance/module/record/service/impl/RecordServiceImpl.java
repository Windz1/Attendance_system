package com.hkywt.attendance.module.record.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hkywt.attendance.common.security.SecurityUtil;
import com.hkywt.attendance.module.clazz.entity.BizClass;
import com.hkywt.attendance.module.clazz.mapper.BizClassMapper;
import com.hkywt.attendance.module.clazz.mapper.BizMemberClassMapper;
import com.hkywt.attendance.module.plan.entity.BizAttendancePlan;
import com.hkywt.attendance.module.plan.mapper.BizAttendancePlanMapper;
import com.hkywt.attendance.module.record.entity.BizAttendanceRecord;
import com.hkywt.attendance.module.record.mapper.BizAttendanceRecordMapper;
import com.hkywt.attendance.module.record.service.RecordService;
import com.hkywt.attendance.module.record.vo.RecordExportRow;
import com.hkywt.attendance.module.record.vo.RecordListVO;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.student.entity.BizStudent;
import com.hkywt.attendance.module.student.mapper.BizStudentMapper;
import com.hkywt.attendance.module.system.entity.SysUser;
import com.hkywt.attendance.module.system.mapper.SysUserMapper;
import com.hkywt.attendance.module.task.entity.BizAttendanceTask;
import com.hkywt.attendance.module.task.mapper.BizAttendanceTaskMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecordServiceImpl implements RecordService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizAttendanceRecordMapper recordMapper;
    private final BizMemberClassMapper memberClassMapper;
    private final BizAttendanceTaskMapper taskMapper;
    private final BizAttendancePlanMapper planMapper;
    private final BizClassMapper classMapper;
    private final BizStudentMapper studentMapper;
    private final SysUserMapper userMapper;

    public RecordServiceImpl(BizAttendanceRecordMapper recordMapper,
                             BizMemberClassMapper memberClassMapper,
                             BizAttendanceTaskMapper taskMapper,
                             BizAttendancePlanMapper planMapper,
                             BizClassMapper classMapper,
                             BizStudentMapper studentMapper,
                             SysUserMapper userMapper) {
        this.recordMapper = recordMapper;
        this.memberClassMapper = memberClassMapper;
        this.taskMapper = taskMapper;
        this.planMapper = planMapper;
        this.classMapper = classMapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<RecordListVO> query(LocalDateTime startTime,
                                    LocalDateTime endTime,
                                    Long classId,
                                    Long planId,
                                    String keyword,
                                    Integer status,
                                    Boolean leaveOnly,
                                    String sortField,
                                    String sortOrder) {
        Integer statusFilter;
        if (Boolean.TRUE.equals(leaveOnly)) {
            statusFilter = 2;
        } else {
            statusFilter = status;
        }
        LambdaQueryWrapper<BizAttendanceRecord> wrapper = new LambdaQueryWrapper<BizAttendanceRecord>()
                .eq(BizAttendanceRecord::getDeleted, 0)
                .eq(classId != null, BizAttendanceRecord::getClassId, classId)
                .eq(planId != null, BizAttendanceRecord::getPlanId, planId)
                .eq(statusFilter != null, BizAttendanceRecord::getAttendanceStatus, statusFilter)
                .orderByDesc(BizAttendanceRecord::getOperateTime);

        if (SecurityUtil.hasRole(RoleCode.MEMBER) && !SecurityUtil.hasRole(RoleCode.ADMIN)) {
            List<Long> myClassIds = memberClassMapper.findClassIdsByMemberUserId(SecurityUtil.currentUserId());
            if (myClassIds.isEmpty()) return Collections.emptyList();
            wrapper.in(BizAttendanceRecord::getClassId, myClassIds);
        }

        List<BizAttendanceRecord> records = recordMapper.selectList(wrapper);
        if (records.isEmpty()) return List.of();

        Map<Long, BizAttendanceTask> taskMap = listToMap(
                taskMapper.selectBatchIds(records.stream().map(BizAttendanceRecord::getTaskId).collect(Collectors.toSet())),
                BizAttendanceTask::getId);
        Map<Long, BizAttendancePlan> planMap = listToMap(
                planMapper.selectBatchIds(records.stream().map(BizAttendanceRecord::getPlanId).collect(Collectors.toSet())),
                BizAttendancePlan::getId);
        Map<Long, BizClass> classMap = listToMap(
                classMapper.selectBatchIds(records.stream().map(BizAttendanceRecord::getClassId).collect(Collectors.toSet())),
                BizClass::getId);
        Map<Long, BizStudent> studentMap = listToMap(
                studentMapper.selectBatchIds(records.stream().map(BizAttendanceRecord::getStudentId).collect(Collectors.toSet())),
                BizStudent::getId);
        Map<Long, SysUser> userMap = listToMap(
                userMapper.selectBatchIds(records.stream().map(BizAttendanceRecord::getOperatorUserId).collect(Collectors.toSet())),
                SysUser::getId);

        String keywordTrim = keyword == null ? "" : keyword.trim();
        Comparator<RecordListVO> comparator = buildComparator(sortField, sortOrder);

        return records.stream()
                .map(r -> toListVO(r, taskMap, planMap, classMap, studentMap, userMap))
                .filter(v -> matchKeyword(v, keywordTrim))
                .filter(v -> matchTime(v, startTime, endTime))
                .sorted(comparator)
                .toList();
    }

    @Override
    public void export(LocalDateTime startTime,
                       LocalDateTime endTime,
                       Long classId,
                       Long planId,
                       String keyword,
                       Integer status,
                       Boolean leaveOnly,
                       String sortField,
                       String sortOrder,
                       HttpServletResponse response) {
        List<RecordListVO> list = query(startTime, endTime, classId, planId, keyword, status, leaveOnly, sortField, sortOrder);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_records.xlsx");
        try {
            List<RecordExportRow> rows = list.stream().map(v -> {
                RecordExportRow row = new RecordExportRow();
                row.setTaskId(v.getTaskId());
                row.setPlanName(v.getPlanName());
                row.setClassName(v.getClassName());
                row.setWindowStartAt(format(v.getWindowStartAt()));
                row.setWindowEndAt(format(v.getWindowEndAt()));
                row.setSubmittedAt(format(v.getSubmittedAt()));
                row.setStudentName(v.getStudentName());
                row.setStudentNo(v.getStudentNo());
                row.setAttendanceStatus(v.getAttendanceStatusText());
                row.setLeaveStatus(v.getLeaveStatus());
                row.setOperatorName(v.getOperatorName());
                return row;
            }).toList();
            EasyExcel.write(response.getOutputStream(), RecordExportRow.class)
                    .autoCloseStream(false)
                    .sheet("考勤记录")
                    .doWrite(rows);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RecordListVO toListVO(BizAttendanceRecord r,
                                  Map<Long, BizAttendanceTask> taskMap,
                                  Map<Long, BizAttendancePlan> planMap,
                                  Map<Long, BizClass> classMap,
                                  Map<Long, BizStudent> studentMap,
                                  Map<Long, SysUser> userMap) {
        BizAttendanceTask task = taskMap.get(r.getTaskId());
        BizAttendancePlan plan = planMap.get(r.getPlanId());
        BizClass clazz = classMap.get(r.getClassId());
        BizStudent student = studentMap.get(r.getStudentId());
        SysUser user = userMap.get(r.getOperatorUserId());

        RecordListVO vo = new RecordListVO();
        vo.setTaskId(r.getTaskId());
        vo.setPlanId(r.getPlanId());
        vo.setClassId(r.getClassId());
        vo.setStudentId(r.getStudentId());
        vo.setPlanName(plan == null ? "-" : valueOrDash(plan.getPlanName()));
        vo.setClassName(clazz == null ? "ID:" + r.getClassId() : valueOrDash(clazz.getClassName()));
        vo.setWindowStartAt(task == null ? null : task.getWindowStartAt());
        vo.setWindowEndAt(task == null ? null : task.getWindowEndAt());
        vo.setSubmittedAt(task == null ? null : task.getSubmittedAt());
        vo.setStudentName(student == null ? "-" : valueOrDash(student.getStudentName()));
        vo.setStudentNo(student == null ? "-" : valueOrDash(student.getStudentNo()));
        vo.setAttendanceStatus(r.getAttendanceStatus());
        vo.setAttendanceStatusText(toStatusText(r.getAttendanceStatus()));
        vo.setLeaveStatus(r.getAttendanceStatus() != null && r.getAttendanceStatus() == 2 ? "已请假" : "未请假");
        vo.setLeavePeriod(r.getAttendanceStatus() != null && r.getAttendanceStatus() == 2
                ? formatWindow(task == null ? null : task.getWindowStartAt(), task == null ? null : task.getWindowEndAt())
                : "-");
        vo.setLeaveReason(r.getAttendanceStatus() != null && r.getAttendanceStatus() == 2 ? valueOrDash(r.getRemark()) : "-");
        vo.setRemark(valueOrDash(r.getRemark()));
        vo.setOperatorName(user == null ? "-" : valueOrDash(user.getRealName()));
        vo.setOperateTime(r.getOperateTime());
        return vo;
    }

    private boolean matchKeyword(RecordListVO row, String keyword) {
        if (keyword.isBlank()) return true;
        return row.getPlanName() != null && row.getPlanName().toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean matchTime(RecordListVO row, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null && endTime == null) return true;
        LocalDateTime compareTime = row.getSubmittedAt() == null ? row.getOperateTime() : row.getSubmittedAt();
        if (compareTime == null) return false;
        if (startTime != null && compareTime.isBefore(startTime)) return false;
        return endTime == null || !compareTime.isAfter(endTime);
    }

    private Comparator<RecordListVO> buildComparator(String sortField, String sortOrder) {
        String field = sortField == null || sortField.isBlank() ? "submittedAt" : sortField;
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        Comparator<RecordListVO> cmp = switch (field) {
            case "windowStartAt" -> Comparator.comparing(RecordListVO::getWindowStartAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "windowEndAt" -> Comparator.comparing(RecordListVO::getWindowEndAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "studentName" -> Comparator.comparing(RecordListVO::getStudentName, Comparator.nullsLast(String::compareTo));
            case "studentNo" -> Comparator.comparing(RecordListVO::getStudentNo, Comparator.nullsLast(String::compareTo));
            case "planName" -> Comparator.comparing(RecordListVO::getPlanName, Comparator.nullsLast(String::compareTo));
            case "className" -> Comparator.comparing(RecordListVO::getClassName, Comparator.nullsLast(String::compareTo));
            case "attendanceStatus" -> Comparator.comparing(RecordListVO::getAttendanceStatus, Comparator.nullsLast(Comparator.naturalOrder()));
            case "operateTime" -> Comparator.comparing(RecordListVO::getOperateTime, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(RecordListVO::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return asc ? cmp : cmp.reversed();
    }

    private String format(LocalDateTime time) {
        return time == null ? "-" : DATE_TIME_FMT.format(time);
    }

    private String formatWindow(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "-";
        return format(start) + " ~ " + format(end);
    }

    private String valueOrDash(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    private <T> Map<Long, T> listToMap(List<T> list, Function<T, Long> idGetter) {
        return list.stream()
                .filter(v -> idGetter.apply(v) != null)
                .collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a));
    }

    private String toStatusText(Integer code) {
        return switch (code == null ? 0 : code) {
            case 1 -> "出勤";
            case 2 -> "请假";
            case 3 -> "迟到";
            case 4 -> "旷课";
            default -> "未知";
        };
    }
}
