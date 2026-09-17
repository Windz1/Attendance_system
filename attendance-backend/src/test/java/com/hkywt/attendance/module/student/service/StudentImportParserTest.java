package com.hkywt.attendance.module.student.service;

import com.alibaba.excel.EasyExcel;
import com.hkywt.attendance.common.exception.BizException;
import com.hkywt.attendance.module.student.dto.StudentImportRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class StudentImportParserTest {

    private final StudentImportParser parser = new StudentImportParser();

    @Test
    void readsFourColumnRosterByHeader() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output)
                .head(List.of(List.of("姓名"), List.of("学号"), List.of("性别"), List.of("班级")))
                .sheet()
                .doWrite(List.of(List.of("测试学生", "20260001", "女", "测试班")));

        List<StudentImportRow> rows = parser.parse(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(1, rows.size());
        assertEquals("20260001", rows.get(0).getStudentNo());
        assertEquals("测试学生", rows.get(0).getStudentName());
        assertEquals("测试班", rows.get(0).getClassName());
    }

    @Test
    void missingRequiredHeadersReturnsClearError() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output).head(List.of(List.of("姓名"), List.of("学号")))
                .sheet().doWrite(List.of(List.of("测试学生", "20260001")));
        BizException error = assertThrows(BizException.class,
                () -> parser.parse(new ByteArrayInputStream(output.toByteArray())));
        assertTrue(error.getMessage().contains("班级"));
    }

    @Test
    void readsSuppliedRosterWhenAvailable() throws Exception {
        String samplePath = System.getProperty("student.sample.path");
        assumeTrue(samplePath != null && Files.isRegularFile(Path.of(samplePath)));
        try (var input = Files.newInputStream(Path.of(samplePath))) {
            List<StudentImportRow> rows = parser.parse(input);
            assertEquals(635, rows.size());
            assertTrue(rows.stream().allMatch(row -> row.getStudentNo() != null
                    && !row.getStudentNo().isBlank()
                    && row.getStudentName() != null && !row.getStudentName().isBlank()
                    && row.getClassName() != null && !row.getClassName().isBlank()));
        }
    }
}
