package com.hkywt.attendance.module.clazz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.common.security.SecurityUtil;
import com.hkywt.attendance.module.clazz.dto.ClassSaveRequest;
import com.hkywt.attendance.module.clazz.entity.BizClass;
import com.hkywt.attendance.module.clazz.mapper.BizClassMapper;
import com.hkywt.attendance.module.clazz.mapper.BizMemberClassMapper;
import com.hkywt.attendance.module.shared.enums.RoleCode;
import com.hkywt.attendance.module.clazz.service.ClassService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ClassServiceImpl implements ClassService {

    private final BizClassMapper classMapper;
    private final BizMemberClassMapper memberClassMapper;

    public ClassServiceImpl(BizClassMapper classMapper, BizMemberClassMapper memberClassMapper) {
        this.classMapper = classMapper;
        this.memberClassMapper = memberClassMapper;
    }

    private void checkAdmin() {
        if (!SecurityUtil.hasRole(RoleCode.ADMIN)) throw new BizException(403, "无权限操作");
    }

    @Override
    public List<BizClass> listAll(String keyword) {
        return classMapper.selectList(new LambdaQueryWrapper<BizClass>()
                .eq(BizClass::getDeleted, 0)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(BizClass::getClassName, keyword).or().like(BizClass::getClassCode, keyword))
                .orderByDesc(BizClass::getId));
    }

    @Override
    public List<BizClass> listMyClasses() {
        List<Long> classIds = memberClassMapper.findClassIdsByMemberUserId(SecurityUtil.currentUserId());
        if (classIds.isEmpty()) return Collections.emptyList();
        return classMapper.selectList(new LambdaQueryWrapper<BizClass>()
                .eq(BizClass::getDeleted, 0)
                .in(BizClass::getId, classIds)
                .orderByAsc(BizClass::getClassName));
    }

    @Override
    public void save(ClassSaveRequest request) {
        checkAdmin();
        BizClass entity = new BizClass();
        BeanUtils.copyProperties(request, entity);
        if (request.getId() == null) {
            Long exists = classMapper.selectCount(new LambdaQueryWrapper<BizClass>()
                    .eq(BizClass::getClassCode, request.getClassCode())
                    .eq(BizClass::getDeleted, 0));
            if (exists > 0) throw new BizException("班级编码已存在");
            entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
            entity.setCreateBy(SecurityUtil.currentUserId());
            classMapper.insert(entity);
            return;
        }
        entity.setId(request.getId());
        entity.setUpdateBy(SecurityUtil.currentUserId());
        classMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        checkAdmin();
        classMapper.update(null, new LambdaUpdateWrapper<BizClass>()
                .eq(BizClass::getId, id)
                .eq(BizClass::getDeleted, 0)
                .set(BizClass::getDeleted, 1));
    }
}
