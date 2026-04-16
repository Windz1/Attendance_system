package com.hkywt.attendance.module.record.service;

import com.hkywt.attendance.module.record.vo.RecordListVO;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordService {
    List<RecordListVO> query(LocalDateTime startTime,
                             LocalDateTime endTime,
                             Long classId,
                             Long planId,
                             String keyword,
                             Integer status,
                             Boolean leaveOnly,
                             String sortField,
                             String sortOrder);

    void export(LocalDateTime startTime,
                LocalDateTime endTime,
                Long classId,
                Long planId,
                String keyword,
                Integer status,
                Boolean leaveOnly,
                String sortField,
                String sortOrder,
                HttpServletResponse response);
}
