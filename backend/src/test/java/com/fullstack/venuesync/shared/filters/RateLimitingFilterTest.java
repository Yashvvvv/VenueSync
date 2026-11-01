package com.fullstack.venuesync.shared.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

/** The quota key is the real client, never a header the caller picks, and the key map stays bounded. */
class RateLimitingFilterTest {

  private final RateLimitingFilter filter = new RateLimitingFilter();

  private int status(Consumer<MockHttpServletRequest> headers) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/published-events");
    request.setRemoteAddr("10.0.0.1"); // the proxy in front, the same for everyone
    headers.accept(request);
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response.getStatus();
  }

  @Test
  void cloudflareClientIpWinsOverAForgedForwardedFor() throws Exception {
    for (int i = 1; i <= 60; i++) {
      int n = i;
      assertEquals(200, status(r -> {
        r.addHeader("CF-Connecting-IP", "203.0.113.7");
        r.addHeader("X-Forwarded-For", "198.51.100." + n + ", 203.0.113.7");
      }));
    }
    assertEquals(429, status(r -> {
      r.addHeader("CF-Connecting-IP", "203.0.113.7");
      r.addHeader("X-Forwarded-For", "198.51.100.200, 203.0.113.7");
    }));
    assertEquals(200, status(r -> r.addHeader("CF-Connecting-IP", "203.0.113.8"))); // someone else
  }

  @Test
  void withoutCloudflareAJunkForwardedForFallsBackToTheConnection() throws Exception {
    String junk = "x".repeat(8000);
    for (int i = 0; i < 60; i++) {
      int n = i;
      assertEquals(200, status(r -> r.addHeader("X-Forwarded-For", junk + n)));
    }
    assertEquals(429, status(r -> r.addHeader("X-Forwarded-For", junk + "again")));
    assertEquals(Map.of("10.0.0.1", 1), keys());
  }

  @Test
  void theKeyMapNeverGrowsPastItsCap() throws Exception {
    for (int i = 0; i < RateLimitingFilter.MAX_CLIENTS + 50; i++) {
      String ip = String.format("10.%d.%d.%d", (i >> 16) & 255, (i >> 8) & 255, i & 255);
      assertEquals(200, status(r -> r.addHeader("CF-Connecting-IP", ip)));
    }
    assertTrue(keys().size() <= RateLimitingFilter.MAX_CLIENTS);
  }

  /** A flood of new addresses used to fill the map, after which every new client went unlimited. */
  @Test
  void aFullMapStillLimitsANewClient() throws Exception {
    flood(RateLimitingFilter.MAX_CLIENTS + 50, () -> { });
    for (int i = 0; i < 60; i++) {
      assertEquals(200, status(r -> r.addHeader("CF-Connecting-IP", "198.51.100.9")));
    }
    assertEquals(429, status(r -> r.addHeader("CF-Connecting-IP", "198.51.100.9")));
  }

  /** Eviction takes the longest-idle client, so a flood can't push out the one that's hammering and reset its quota. */
  @Test
  void floodingNewAddressesCannotResetAnActiveClient() throws Exception {
    for (int i = 0; i < 60; i++) {
      assertEquals(200, status(r -> r.addHeader("CF-Connecting-IP", "203.0.113.7")));
    }
    flood(RateLimitingFilter.MAX_CLIENTS + 50,
        () -> assertEquals(429, status(r -> r.addHeader("CF-Connecting-IP", "203.0.113.7"))));
  }

  /** One request from each of {@code n} new addresses, running {@code every100} after each hundred. */
  private void flood(int n, ThrowingRunnable every100) throws Exception {
    for (int i = 0; i < n; i++) {
      String ip = String.format("10.%d.%d.%d", (i >> 16) & 255, (i >> 8) & 255, i & 255);
      status(r -> r.addHeader("CF-Connecting-IP", ip));
      if (i % 100 == 99) {
        every100.run();
      }
    }
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  /** Keys with a 1 each, just to compare sizes and names. */
  @SuppressWarnings("unchecked")
  private Map<String, Integer> keys() {
    Map<String, ?> counts = (Map<String, ?>) ReflectionTestUtils.getField(filter, "requestCounts");
    return counts.keySet().stream().collect(java.util.stream.Collectors.toMap(k -> k, k -> 1));
  }
}
