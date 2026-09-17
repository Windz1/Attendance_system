package com.hkywt.attendance.module.student.service;

import com.alibaba.excel.EasyExcel;
import com.hkywt.attendance.common.security.SecurityUser;
import com.hkywt.attendance.module.clazz.entity.BizClass;
import com.hkywt.attendance.module.clazz.mapper.BizClassMapper;
import com.hkywt.attendance.module.clazz.mapper.BizMemberClassMapper;
import com.hkywt.attendance.module.student.entity.BizStudentImportLog;
import com.hkywt.attendance.module.student.entity.BizStudent;
import com.hkywt.attendance.module.student.mapper.BizStudentImportLogMapper;
import com.hkywt.attendance.module.student.mapper.BizStudentMapper;
import com.hkywt.attendance.module.student.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

class StudentImportServiceTest {

    private static <T> T mock(Class<T> type) {
        return Mockito.mock(type, withSettings().mockMaker(MockMakers.SUBCLASS));
    }

    @BeforeEach
    void authenticateAdmin() {
        SecurityUser admin = SecurityUser.builder().userId(1L).userType(1).roles(Set.of()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void downloadedTemplateHasImportableHeaders() {
        StudentServiceImpl service = new StudentServiceImpl(
                mock(BizStudentMapper.class), mock(BizClassMapper.class),
                mock(BizMemberClassMapper.class), mock(BizStudentImportLogMapper.class));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadImportTemplate(response);

        assertTrue(response.getContentType().contains("spreadsheetml"));
        assertTrue(response.getContentAsByteArray().length > 0);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        EasyExcel.write(data).head(List.of(List.of("学号"), List.of("姓名"), List.of("班级")))
                .sheet().doWrite(List.of(List.of("20260001", "测试学生", "测试班")));
        assertEquals(1, new StudentImportParser()
                .parse(new ByteArrayInputStream(data.toByteArray())).size());
        List<java.util.Map<Integer, String>> template = EasyExcel.read(new ByteArrayInputStream(response.getContentAsByteArray()))
                .headRowNumber(0).sheet().doReadSync();
        assertEquals("学号", template.get(0).get(0));
        assertEquals("姓名", template.get(0).get(1));
        assertEquals("班级", template.get(0).get(6));
    }

    @Test
    void importsEveryRowOfSuppliedRosterWhenAvailable() throws Exception {
        String samplePath = System.getProperty("student.sample.path");
        assumeTrue(samplePath != null && Files.isRegularFile(Path.of(samplePath)));
        BizStudentMapper students = mock(BizStudentMapper.class);
        BizClassMapper classes = mock(BizClassMapper.class);
        BizStudentImportLogMapper logs = mock(BizStudentImportLogMapper.class);
        when(classes.selectList(any())).thenReturn(List.of());
        AtomicLong nextClassId = new AtomicLong();
        doAnswer(call -> {
            BizClass clazz = call.getArgument(0);
            clazz.setId(nextClassId.incrementAndGet());
            return 1;
        }).when(classes).insert(any(BizClass.class));
        StudentServiceImpl service = new StudentServiceImpl(students, classes,
                mock(BizMemberClassMapper.class), logs);
        byte[] bytes = Files.readAllBytes(Path.of(samplePath));
        MockMultipartFile file = new MockMultipartFile("file", "新生名单.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        String result = service.importXlsx(file, 2);

        assertTrue(result.contains("成功635条，失败0条"));
        verify(students, times(635)).insert(any(BizStudent.class));
        verify(classes, times(13)).insert(any(BizClass.class));
        verify(logs).insert(argThat((BizStudentImportLog log) -> log.getTotalCount() == 635
                && log.getSuccessCount() == 635 && log.getFailCount() == 0));
    }
}
