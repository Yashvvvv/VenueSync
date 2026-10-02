package com.fullstack.venuesync.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DateTimeException;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TimezoneEnvironmentPostProcessorTest {

  private final TimeZone original = TimeZone.getDefault();

  @AfterEach
  void restore() {
    TimeZone.setDefault(original);
  }

  @Test
  void pinsTheJvmToAppTimezone() {
    new TimezoneEnvironmentPostProcessor().postProcessEnvironment(
        new MockEnvironment().withProperty("app.timezone", "Asia/Kolkata"), null);

    assertEquals("Asia/Kolkata", TimeZone.getDefault().getID());
  }

  @Test
  void defaultsToIndiaWhenUnset() {
    new TimezoneEnvironmentPostProcessor().postProcessEnvironment(new MockEnvironment(), null);

    assertEquals("Asia/Kolkata", TimeZone.getDefault().getID());
  }

  @Test
  void failsFastOnATypoInsteadOfFallingBackToGmt() {
    var environment = new MockEnvironment().withProperty("app.timezone", "Asia/Kolkatta");

    assertThrows(DateTimeException.class,
        () -> new TimezoneEnvironmentPostProcessor().postProcessEnvironment(environment, null));
  }
}
