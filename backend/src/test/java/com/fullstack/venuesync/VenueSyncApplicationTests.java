package com.fullstack.venuesync;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VenueSyncApplicationTests {

  @Autowired private DataSource dataSource;

  @Test
  void contextLoads() {
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
