package com.fullstack.venuesync.shared.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Makes every time in API responses carry the {@code app.timezone} offset (ADR-003):
 * {@code 2026-10-05T19:00:00+05:30}. Same wall clock as before, but unambiguous to clients in other
 * zones instead of making them assume India.
 *
 * <p>The zone itself is {@code app.timezone}, pinned onto the JVM by
 * {@link TimezoneEnvironmentPostProcessor} before startup — so this reads the JVM default.
 */
@Configuration
public class TimezoneConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer wallClockWithOffset() {
    return builder -> builder.serializerByType(LocalDateTime.class, new WallClockOffsetSerializer(ZoneId.systemDefault()));
  }

  /** Writes a wall-clock {@link LocalDateTime} with the offset it has in {@code zone} on that date. */
  public static class WallClockOffsetSerializer extends StdSerializer<LocalDateTime> {

    private final ZoneId zone;

    public WallClockOffsetSerializer(ZoneId zone) {
      super(LocalDateTime.class);
      this.zone = zone;
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeString(value.atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
  }
}
