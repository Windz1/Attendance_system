package com.hkywt.attendance.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class SystemTimeConfig {

    @Value("${attendance.timezone:Asia/Shanghai}")
    private String timezone;

    @PostConstruct
    public void configureSystemTimezone() {
        ZoneId zoneId = ZoneId.of(timezone);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }
}
