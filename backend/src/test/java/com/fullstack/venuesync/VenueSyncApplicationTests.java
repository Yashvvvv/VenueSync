package com.fullstack.venuesync;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fullstack.venuesync.shared.config.TimezoneConfig;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VenueSyncApplicationTests {

  @Autowired private DataSource dataSource;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void contextLoads() {
  }

  /**
   * Times leave the API with the app.timezone offset (ADR-003). Uses Spring's real ObjectMapper, so it
   * also proves the customizer wins over Jackson's default LocalDateTime serializer. Tests run in UTC.
   */
  @Test
  void apiTimesCarryTheAppTimezoneOffset() throws Exception {
    assertEquals("\"2026-10-05T19:00:00Z\"", objectMapper.writeValueAsString(LocalDateTime.of(2026, 10, 5, 19, 0)));
  }

  @Test
  void offsetFollowsTheZoneOnThatDate() {
    assertAll(
        () -> assertEquals("\"2026-10-05T19:00:00+05:30\"", write("Asia/Kolkata", LocalDateTime.of(2026, 10, 5, 19, 0))),
        // DST: same zone, different offset per date
        () -> assertEquals("\"2026-07-01T19:00:00+01:00\"", write("Europe/London", LocalDateTime.of(2026, 7, 1, 19, 0))),
        () -> assertEquals("\"2026-12-01T19:00:00Z\"", write("Europe/London", LocalDateTime.of(2026, 12, 1, 19, 0))));
  }

  private static String write(String zone, LocalDateTime time) throws Exception {
    var serializer = new TimezoneConfig.WallClockOffsetSerializer(ZoneId.of(zone));
    return new ObjectMapper().registerModule(new SimpleModule().addSerializer(LocalDateTime.class, serializer))
        .writeValueAsString(time);
  }

  /**
   * Guards a failure mode that context-loading does not catch: Hibernate logs a
   * DDL error and carries on, so a table can be missing from the test schema
   * while every test still passes. That is exactly what happened to qr_codes -
   * H2 2.x reserves VALUE, the QrCode entity has a `value` column, and the
   * create statement failed silently for as long as it did because the unit
   * tests mock their repositories. Fixed with NON_KEYWORDS=VALUE in the test
   * datasource URL.
   *
   * <p>If another entity ever picks a column name H2 reserves, add it here.
   */
  @Test
  void qrCodesTableExists() {
    assertDoesNotThrow(() -> {
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement()) {
        statement.executeQuery("SELECT COUNT(*) FROM qr_codes").close();
      }
    }, "qr_codes is missing from the test schema - check the DDL warnings in the build log");
  }
}
