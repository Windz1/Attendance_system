package com.hkywt.attendance.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_CLASS_PHOTOS = 9;
    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BizAttendanceTaskMapper taskMapper;
    private final BizAttendancePlanMapper planMapper;
    private final BizAttendancePlanClassMapper planClassMapper;
    private final BizMemberClassMapper memberClassMapper;
    private final BizAttendanceRecordMapper recordMapper;
    private final BizStudentMapper studentMapper;

    @Value("${attendance.class-photo.root-dir:uploads/class-photos}")
    private String classPhotoRootDir;

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
        vo.setClassPhotoUrls(records.stream()
                .map(BizAttendanceRecord::getClassPhotoUrl)
                .filter(v -> v != null && !v.isBlank())
                .map(this::parseClassPhotoUrls)
                .filter(v -> !v.isEmpty())
                .findFirst()
                .orElse(List.of()));
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
    public String uploadClassPhoto(Long taskId, MultipartFile file) {
        BizAttendanceTask task = requireAccessibleTask(taskId);
        requireTaskWithinWindow(task, false);
        if (file == null || file.isEmpty()) throw new BizException("上传图片不能为空");
        if (file.getSize() > MAX_IMAGE_BYTES) throw new BizException("图片不能超过10MB");
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new BizException("仅支持 jpg、jpeg、png、webp 格式");
        }
        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (Exception e) {
            throw new BizException("图片读取失败，请重新上传");
        }
        if (!matchesImageSignature(ext, imageBytes)) throw new BizException("图片内容与文件格式不匹配");

        LocalDateTime now = LocalDateTime.now();
        String year = YEAR_FMT.format(now);
        String month = MONTH_FMT.format(now);
        Path root = classPhotoRoot();
        Path targetDir = root.resolve(year).resolve(month).normalize();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path targetFile = targetDir.resolve(fileName).normalize();
        if (!targetFile.startsWith(root)) throw new BizException("非法文件路径");

        try {
            Files.createDirectories(targetDir);
            Files.write(targetFile, imageBytes);
        } catch (Exception e) {
            throw new BizException("图片上传失败，请稍后重试");
        }
        return "/api/tasks/class-photo/" + year + "/" + month + "/" + fileName;
    }

    @Override
    public Resource loadClassPhoto(String year, String month, String filename) {
        if (year == null || !year.matches("\\d{4}")) throw new BizException(400, "无效年份");
        if (month == null || !month.matches("\\d{2}")) throw new BizException(400, "无效月份");
        if (filename == null || !filename.matches("[a-zA-Z0-9._-]+")) throw new BizException(400, "无效文件名");

        Path root = classPhotoRoot();
        Path target = root.resolve(year).resolve(month).resolve(filename).normalize();
        if (!target.startsWith(root)) throw new BizException(400, "非法文件路径");
        if (!Files.exists(target)) throw new BizException(404, "图片不存在");
        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BizException(404, "图片不可读");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BizException(500, "读取图片失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(TaskSubmitRequest request) {
        BizAttendanceTask task = requireAccessibleTask(request.getTaskId());
        requireTaskWithinWindow(task, true);

        Set<Long> validStudentIds = studentMapper.selectList(new LambdaQueryWrapper<BizStudent>()
                        .eq(BizStudent::getClassId, task.getClassId())
                        .eq(BizStudent::getDeleted, 0))
                .stream().map(BizStudent::getId).collect(Collectors.toSet());

        Set<Long> submittedStudentIds = request.getRecords().stream()
                .map(TaskSubmitRequest.StudentMark::getStudentId)
                .collect(Collectors.toSet());
        if (submittedStudentIds.size() != request.getRecords().size() || !submittedStudentIds.equals(validStudentIds)) {
            throw new BizException("提交记录必须完整包含班级全部学生，且不能重复");
        }

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
                record.setClassPhotoUrl(serializeClassPhotoUrls(request.getClassPhotoUrls()));
                record.setOperatorUserId(SecurityUtil.currentUserId());
                record.setOperateTime(LocalDateTime.now());
                recordMapper.insert(record);
            } else {
                record.setAttendanceStatus(row.getAttendanceStatus());
                record.setIsDefaultMarked(row.getAttendanceStatus() == 1 ? 1 : 0);
                record.setRemark(row.getRemark());
                record.setClassPhotoUrl(serializeClassPhotoUrls(request.getClassPhotoUrls()));
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

    private Path classPhotoRoot() {
        Path configured = Paths.get(classPhotoRootDir);
        if (!configured.isAbsolute()) configured = Paths.get(System.getProperty("user.dir")).resolve(configured);
        return configured.toAbsolutePath().normalize();
    }

    private String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String serializeClassPhotoUrls(List<String> classPhotoUrls) {
        if (classPhotoUrls == null || classPhotoUrls.isEmpty()) throw new BizException("班级照片不能为空");
        List<String> sanitized = classPhotoUrls.stream()
                .map(v -> v == null ? "" : v.trim())
                .filter(v -> !v.isBlank())
                .distinct()
                .toList();
        if (sanitized.isEmpty()) throw new BizException("班级照片不能为空");
        if (sanitized.size() > MAX_CLASS_PHOTOS) throw new BizException("班级照片最多上传9张");
        if (sanitized.stream().anyMatch(v -> !v.matches("/api/tasks/class-photo/\\d{4}/\\d{2}/[a-f0-9]{32}\\.(jpg|jpeg|png|webp)"))) {
            throw new BizException("班级照片地址无效");
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (Exception e) {
            throw new BizException("班级照片保存失败");
        }
    }

    private List<String> parseClassPhotoUrls(String value) {
        if (value == null || value.isBlank()) return List.of();
        String trim = value.trim();
        if (trim.startsWith("[") && trim.endsWith("]")) {
            try {
                List<String> list = OBJECT_MAPPER.readValue(trim, new TypeReference<List<String>>() {});
                return list.stream()
                        .map(v -> v == null ? "" : v.trim())
                        .filter(v -> !v.isBlank())
                        .distinct()
                        .toList();
            } catch (Exception ignored) {
                return List.of();
            }
        }
        return List.of(trim);
    }

    private BizAttendanceTask requireAccessibleTask(Long taskId) {
        if (taskId == null) throw new BizException("任务ID不能为空");
        BizAttendanceTask task = taskMapper.selectById(taskId);
        if (task == null || Integer.valueOf(1).equals(task.getDeleted())) throw new BizException("任务不存在");
        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) {
            List<Long> myClassIds = memberClassMapper.findClassIdsByMemberUserId(SecurityUtil.currentUserId());
            if (!myClassIds.contains(task.getClassId())) throw new BizException(403, "无权限操作该班级任务");
        }
        return task;
    }

    private void requireTaskWithinWindow(BizAttendanceTask task, boolean submitting) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getWindowStartAt() == null || now.isBefore(task.getWindowStartAt())) {
            throw new BizException(400, "任务尚未开始");
        }
        if (task.getWindowEndAt() == null || now.isAfter(task.getWindowEndAt())) {
            throw new BizException(400, "任务已超过提交时间");
        }
        if (submitting && Integer.valueOf(3).equals(task.getTaskStatus()) && !Integer.valueOf(1).equals(task.getAllowModify())) {
            throw new BizException(400, "任务已提交且不允许修改");
        }
    }

    private boolean matchesImageSignature(String ext, byte[] bytes) {
        if (bytes == null || bytes.length < 12) return false;
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        }
        if ("png".equals(ext)) {
            int[] signature = {0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
            for (int i = 0; i < signature.length; i++) {
                if ((bytes[i] & 0xff) != signature[i]) return false;
            }
            return true;
        }
        return "webp".equals(ext)
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
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
