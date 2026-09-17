package com.hkywt.attendance.module.auth.service;

import com.hkywt.attendance.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级单机登录防爆破。多实例部署时应替换为 Redis 等共享存储。
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TRACKED_KEYS = 10_000;
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Value("${security.login.max-failures:5}")
    private int maxFailures;

    @Value("${security.login.window-minutes:15}")
    private long windowMinutes;

    @Value("${security.login.lock-minutes:15}")
    private long lockMinutes;

    public String key(String username, String clientAddress) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String normalizedAddress = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress.trim();
        return normalizedAddress + ":" + normalizedUsername;
    }

    public void assertAllowed(String key) {
        AttemptState state = attempts.get(key);
        if (state == null) return;
        Instant now = Instant.now();
        if (state.lockedUntil != null && now.isBefore(state.lockedUntil)) {
            throw new BizException(429, "登录失败次数过多，请稍后再试");
        }
        if (state.lockedUntil != null || now.isAfter(state.windowStartedAt.plusSeconds(windowMinutes * 60))) {
            attempts.remove(key, state);
        }
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        if (!attempts.containsKey(key) && attempts.size() >= MAX_TRACKED_KEYS) {
            throw new BizException(429, "登录请求过多，请稍后再试");
        }
        attempts.compute(key, (ignored, oldState) -> {
            AttemptState state = oldState;
            if (state == null || now.isAfter(state.windowStartedAt.plusSeconds(windowMinutes * 60))) {
                state = new AttemptState(now);
            }
            state.failures++;
            if (state.failures >= maxFailures) {
                state.lockedUntil = now.plusSeconds(lockMinutes * 60);
            }
            return state;
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    @Scheduled(fixedDelayString = "${security.login.cleanup-milliseconds:600000}")
    public void cleanupExpiredAttempts() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            if (state.lockedUntil != null) return !now.isBefore(state.lockedUntil);
            return now.isAfter(state.windowStartedAt.plusSeconds(windowMinutes * 60));
        });
    }

    private static final class AttemptState {
        private final Instant windowStartedAt;
        private int failures;
        private Instant lockedUntil;

        private AttemptState(Instant windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }
    }
}
