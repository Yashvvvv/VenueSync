package com.fullstack.venuesync.shared.config;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Pins the JVM default time zone to {@code app.timezone} (ADR-003) at the earliest point the
 * configuration is readable: after properties (and backend/.env) load, before any bean or JDBC
 * connection exists.
 *
 * <p>It has to be this early. The Postgres driver sends the JVM zone as the session TimeZone when it
 * connects; left at the OS default, Windows' India zone surfaces as the legacy alias
 * {@code Asia/Calcutta}, which current Postgres rejects, so startup failed before any
 * {@code @PostConstruct} could fix it. Registered in META-INF/spring.factories.
 */
public class TimezoneEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final String PROPERTY = "app.timezone";
  static final String DEFAULT_ZONE = "Asia/Kolkata";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    // ZoneId.of fails fast on a typo; TimeZone.getTimeZone alone would silently fall back to GMT.
    ZoneId zone = ZoneId.of(environment.getProperty(PROPERTY, DEFAULT_ZONE));
    TimeZone.setDefault(TimeZone.getTimeZone(zone));
  }
}
