package com.hkywt.attendance.module.student.service;

import com.alibaba.excel.EasyExcel;
import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.module.student.dto.StudentImportRow;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 按中文表头而非固定列号解析，兼容四列新生名单和完整模板。 */
public class StudentImportParser {

    private static final Map<String, String> HEADER_FIELDS = Map.ofEntries(
            Map.entry("学号", "studentNo"), Map.entry("学生学号", "studentNo"),
            Map.entry("姓名", "studentName"), Map.entry("学生姓名", "studentName"),
            Map.entry("性别", "gender"), Map.entry("年级", "grade"),
            Map.entry("学院", "college"), Map.entry("专业", "major"),
            Map.entry("班级", "className"), Map.entry("班级名称", "className"),
            Map.entry("手机号", "phone"), Map.entry("联系电话", "phone"),
            Map.entry("状态", "status")
    );

    public List<StudentImportRow> parse(InputStream inputStream) {
        List<Map<Integer, String>> sheet;
        try {
            sheet = EasyExcel.read(inputStream).headRowNumber(0).sheet().doReadSync();
        } catch (Exception e) {
            throw new BizException("无法读取 Excel，请确认文件是有效的 .xlsx 格式");
        }
        if (sheet.isEmpty()) throw new BizException("Excel 没有表头或数据");

        Map<Integer, String> columns = new HashMap<>();
        Set<String> found = new HashSet<>();
        for (Map.Entry<Integer, String> cell : sheet.get(0).entrySet()) {
            String header = normalize(cell.getValue());
            String field = HEADER_FIELDS.get(header);
            if (field == null) continue;
            if (!found.add(field)) throw new BizException("Excel 存在重复表头：" + cell.getValue());
            columns.put(cell.getKey(), field);
        }
        if (!found.containsAll(Set.of("studentNo", "studentName", "className"))) {
            throw new BizException("Excel 必须包含“学号、姓名、班级”三列表头；请下载导入模板");
        }

        List<StudentImportRow> rows = new ArrayList<>();
        for (int index = 1; index < sheet.size(); index++) {
            Map<Integer, String> source = sheet.get(index);
            if (source == null || source.values().stream().allMatch(value -> normalize(value).isEmpty())) continue;
            StudentImportRow row = new StudentImportRow();
            columns.forEach((column, field) -> setField(row, field, clean(source.get(column))));
            rows.add(row);
        }
        if (rows.isEmpty()) throw new BizException("Excel 没有可导入的学生数据");
        return rows;
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private String normalize(String value) {
        return clean(value).replace("\uFEFF", "").replaceAll("[\\s　]+", "");
    }

    private void setField(StudentImportRow row, String field, String value) {
        switch (field) {
            case "studentNo" -> row.setStudentNo(value);
            case "studentName" -> row.setStudentName(value);
            case "gender" -> row.setGender(value);
            case "grade" -> row.setGrade(value);
            case "college" -> row.setCollege(value);
            case "major" -> row.setMajor(value);
            case "className" -> row.setClassName(value);
            case "phone" -> row.setPhone(value);
            case "status" -> row.setStatus(value);
            default -> throw new IllegalArgumentException("Unknown import field: " + field);
        }
    }
}
