package com.hkywt.attendance.module.student.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.common.security.SecurityUtil;
import com.hkywt.attendance.module.clazz.entity.BizClass;
import com.hkywt.attendance.module.clazz.mapper.BizClassMapper;
import com.hkywt.attendance.module.clazz.mapper.BizMemberClassMapper;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.student.dto.StudentImportRow;
import com.hkywt.attendance.module.student.dto.StudentSaveRequest;
import com.hkywt.attendance.module.student.entity.BizStudent;
import com.hkywt.attendance.module.student.entity.BizStudentImportLog;
import com.hkywt.attendance.module.student.mapper.BizStudentImportLogMapper;
import com.hkywt.attendance.module.student.mapper.BizStudentMapper;
import com.hkywt.attendance.module.student.service.StudentService;
import com.hkywt.attendance.module.student.service.StudentImportParser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class StudentServiceImpl implements StudentService {

    private final BizStudentMapper studentMapper;
    private final BizClassMapper classMapper;
    private final BizMemberClassMapper memberClassMapper;
    private final BizStudentImportLogMapper importLogMapper;

    public StudentServiceImpl(BizStudentMapper studentMapper, BizClassMapper classMapper, BizMemberClassMapper memberClassMapper, BizStudentImportLogMapper importLogMapper) {
        this.studentMapper = studentMapper;
        this.classMapper = classMapper;
        this.memberClassMapper = memberClassMapper;
        this.importLogMapper = importLogMapper;
    }

    private void checkAdmin() {
        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) {
            throw new BizException(403, "无权限操作");
        }
    }

    @Override
    public void save(StudentSaveRequest request) {
        checkAdmin();
        BizStudent student = new BizStudent();
        BeanUtils.copyProperties(request, student);
        if (request.getId() == null) {
            Long exists = studentMapper.selectCount(new LambdaQueryWrapper<BizStudent>().eq(BizStudent::getStudentNo, request.getStudentNo()).eq(BizStudent::getDeleted, 0));
            if (exists > 0) throw new BizException("学号已存在");
            student.setCreateBy(SecurityUtil.currentUserId());
            studentMapper.insert(student);
        } else {
            student.setId(request.getId());
            student.setUpdateBy(SecurityUtil.currentUserId());
            studentMapper.updateById(student);
        }
    }

    @Override
    public void delete(Long studentId) {
        checkAdmin();
        studentMapper.update(null, new LambdaUpdateWrapper<BizStudent>().eq(BizStudent::getId, studentId).set(BizStudent::getDeleted, 1));
    }

    @Override
    public List<BizStudent> list(String keyword, Long classId) {
        LambdaQueryWrapper<BizStudent> wrapper = new LambdaQueryWrapper<BizStudent>()
                .eq(BizStudent::getDeleted, 0)
                .eq(classId != null, BizStudent::getClassId, classId)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(BizStudent::getStudentName, keyword).or().like(BizStudent::getStudentNo, keyword))
                .orderByDesc(BizStudent::getId);

        if (SecurityUtil.hasRole(RoleCode.MEMBER) && !SecurityUtil.hasRole(RoleCode.ADMIN)) {
            List<Long> myClassIds = memberClassMapper.findClassIdsByMemberUserId(SecurityUtil.currentUserId());
            if (myClassIds.isEmpty()) return Collections.emptyList();
            wrapper.in(BizStudent::getClassId, myClassIds);
        }
        return studentMapper.selectList(wrapper);
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        checkAdmin();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" +
                URLEncoder.encode("学生导入模板.xlsx", StandardCharsets.UTF_8));
        List<List<String>> headers = List.of("学号", "姓名", "性别", "年级", "学院", "专业", "班级", "手机号", "状态")
                .stream().map(List::of).toList();
        try {
            EasyExcel.write(response.getOutputStream()).head(headers).sheet("学生导入模板").doWrite(Collections.emptyList());
        } catch (IOException e) {
            throw new BizException("生成导入模板失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importXlsx(MultipartFile file, Integer importMode) {
        checkAdmin();
        if (file == null || file.isEmpty()) {
            throw new BizException("导入文件不能为空");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BizException("仅支持xlsx文件");
        }

        List<StudentImportRow> rows;
        try {
            rows = new StudentImportParser().parse(file.getInputStream());
        } catch (IOException e) {
            throw new BizException("读取Excel失败:" + e.getMessage());
        }

        String batchNo = "IMP" + System.currentTimeMillis();
        int success = 0;
        int fail = 0;
        Map<String, Long> classCodeMap = new HashMap<>();
        Map<String, Long> classNameMap = new HashMap<>();
        Set<String> seenStudentNos = new HashSet<>();
        classMapper.selectList(new LambdaQueryWrapper<BizClass>().eq(BizClass::getDeleted, 0))
                .forEach(v -> {
                    classCodeMap.put(v.getClassCode(), v.getId());
                    classNameMap.put(v.getClassName(), v.getId());
                });

        for (StudentImportRow row : rows) {
            if (row.getStudentNo() == null || row.getStudentNo().isBlank()
                    || row.getStudentName() == null || row.getStudentName().isBlank()
                    || row.getClassName() == null || row.getClassName().isBlank()) {
                fail++;
                continue;
            }
            if (!seenStudentNos.add(row.getStudentNo())) {
                fail++;
                continue;
            }
            Long classId = classNameMap.get(row.getClassName());
            if (classId == null) {
                classId = classCodeMap.get(row.getClassName());
            }
            if (classId == null) {
                BizClass c = new BizClass();
                c.setClassCode(row.getClassName());
                c.setClassName(row.getClassName());
                c.setMajor(row.getMajor());
                c.setGrade(row.getGrade());
                c.setCollege(row.getCollege());
                c.setStatus(1);
                c.setCreateBy(SecurityUtil.currentUserId());
                classMapper.insert(c);
                classId = c.getId();
                classCodeMap.put(c.getClassCode(), classId);
                classNameMap.put(c.getClassName(), classId);
            }
            BizStudent exists = studentMapper.selectOne(new LambdaQueryWrapper<BizStudent>().eq(BizStudent::getStudentNo, row.getStudentNo()).eq(BizStudent::getDeleted, 0).last("limit 1"));
            if (exists != null && (importMode == null || importMode == 1)) {
                fail++;
                continue;
            }
            BizStudent target = exists == null ? new BizStudent() : exists;
            target.setStudentNo(row.getStudentNo());
            target.setStudentName(row.getStudentName());
            target.setClassId(classId);
            target.setGender(parseGender(row.getGender()));
            target.setMajor(row.getMajor());
            target.setGrade(row.getGrade());
            target.setCollege(row.getCollege());
            target.setPhone(row.getPhone());
            target.setStatus(parseStatus(row.getStatus()));
            if (exists == null) {
                target.setCreateBy(SecurityUtil.currentUserId());
                studentMapper.insert(target);
            } else {
                target.setUpdateBy(SecurityUtil.currentUserId());
                studentMapper.updateById(target);
            }
            success++;
        }

        BizStudentImportLog log = new BizStudentImportLog();
        log.setImportBatchNo(batchNo);
        log.setFileName(file.getOriginalFilename());
        log.setImportMode(importMode == null ? 1 : importMode);
        log.setTotalCount(rows.size());
        log.setSuccessCount(success);
        log.setFailCount(fail);
        log.setResultMessage("导入完成，成功" + success + "条，失败" + fail + "条");
        log.setOperatorUserId(SecurityUtil.currentUserId());
        importLogMapper.insert(log);

        return log.getResultMessage();
    }

    private int parseGender(String v) {
        if (v == null) return 0;
        return switch (v.trim()) {
            case "男" -> 1;
            case "女" -> 2;
            case "1" -> 1;
            case "2" -> 2;
            default -> 0;
        };
    }

    private int parseStatus(String v) {
        if (v == null || v.isBlank()) return 1;
        return ("离校".equals(v) || "0".equals(v)) ? 0 : 1;
    }
}
