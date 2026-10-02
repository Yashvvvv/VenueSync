package com.fullstack.venuesync.shared.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The single source of VenueSync's time zone ({@code app.timezone}, default Asia/Kolkata).
 * See ADR-003: every stored time is a wall-clock time in this zone (India-only by design).
 *
 * <ul>
 *   <li>Pins the JVM default so {@code LocalDateTime.now()} is "now" in that zone.</li>
 *   <li>Makes every time in API responses carry the zone's offset
 *       ({@code 2026-10-05T19:00:00+05:30}): same wall clock as before, but unambiguous to
 *       clients in other zones instead of making them assume India.</li>
 * </ul>
 */
@Configuration
@Slf4j
public class TimezoneConfig {

    private final ZoneId zone;

    public TimezoneConfig(@Value("${app.timezone:Asia/Kolkata}") String timezone) {
        this.zone = ZoneId.of(timezone); // fails startup on a typo; TimeZone.getTimeZone would silently fall back to GMT
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        log.info("JVM timezone set to: {}", zone);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer wallClockWithOffset() {
        return builder -> builder.serializerByType(LocalDateTime.class, new WallClockOffsetSerializer(zone));
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
