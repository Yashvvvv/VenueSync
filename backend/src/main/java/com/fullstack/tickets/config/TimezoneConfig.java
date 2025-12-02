package com.fullstack.tickets.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Configuration to set the JVM timezone for the application.
 * 
 * IMPORTANT: For the "wall clock time" approach to work correctly,
 * the JVM timezone MUST match the timezone where events are happening.
 * 
 * This ensures that LocalDateTime.now() returns the expected local time
 * when validating sales periods and event times.
 */
@Configuration
@Slf4j
public class TimezoneConfig {

    @Value("${app.timezone:Asia/Kolkata}")
    private String timezone;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        log.info("JVM timezone set to: {} ({})", timezone, TimeZone.getDefault().getDisplayName());
    }
}
