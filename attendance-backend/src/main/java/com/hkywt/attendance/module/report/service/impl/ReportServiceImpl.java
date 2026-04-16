package com.hkywt.attendance.module.report.service.impl;

import com.hkywt.attendance.module.record.service.RecordService;
import com.hkywt.attendance.module.record.vo.RecordListVO;
import com.hkywt.attendance.module.report.service.ReportService;
import com.hkywt.attendance.module.report.vo.ClassStatusStatVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final RecordService recordService;

    public ReportServiceImpl(RecordService recordService) {
        this.recordService = recordService;
    }

    @Override
    public List<ClassStatusStatVO> classStatusStat(LocalDate startDate, LocalDate endDate, Long classId) {
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        List<RecordListVO> records = recordService.query(startTime, endTime, classId, null, null, null, null, null, null);
        Map<String, Long> map = records.stream()
                .collect(Collectors.groupingBy(v -> v.getClassId() + "_" + v.getAttendanceStatus(), Collectors.counting()));
        return map.entrySet().stream().map(e -> {
            String[] arr = e.getKey().split("_");
            return new ClassStatusStatVO(Long.parseLong(arr[0]), Integer.parseInt(arr[1]), e.getValue());
        }).toList();
    }
}
