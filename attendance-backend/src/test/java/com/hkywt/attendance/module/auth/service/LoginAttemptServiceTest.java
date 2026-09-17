package com.hkywt.attendance.module.auth.service;

import com.hkywt.attendance.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
        ReflectionTestUtils.setField(service, "maxFailures", 5);
        ReflectionTestUtils.setField(service, "windowMinutes", 15L);
        ReflectionTestUtils.setField(service, "lockMinutes", 15L);
    }

    @Test
    void locksAfterConfiguredFailures() {
        String key = service.key("admin", "127.0.0.1");
        for (int i = 0; i < 5; i++) service.recordFailure(key);

        BizException exception = assertThrows(BizException.class, () -> service.assertAllowed(key));
        assertEquals(429, exception.getCode());
    }

    @Test
    void successfulLoginClearsFailures() {
        String key = service.key("admin", "127.0.0.1");
        for (int i = 0; i < 4; i++) service.recordFailure(key);
        service.recordSuccess(key);

        assertDoesNotThrow(() -> service.assertAllowed(key));
    }
}
