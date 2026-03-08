package com.fullstack.venuesync.shared.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for public API endpoints.
 * Limits each IP to a configurable number of requests per minute.
 * Uses a sliding window approach with automatic cleanup.
 *
 * <p>This is suitable for free-tier single-instance deployments.
 * For multi-instance deployments, use Redis-backed rate limiting.</p>
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS_PER_MINUTE = 60;
  private static final long WINDOW_MS = 60_000L;
  private static final long CLEANUP_INTERVAL_MS = 300_000L; // 5 minutes

  private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();
  private volatile long lastCleanup = System.currentTimeMillis();

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String clientIp = getClientIp(request);
    long now = System.currentTimeMillis();

    // Periodic cleanup of stale entries
    if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
      cleanup(now);
      lastCleanup = now;
    }

    RateLimitEntry entry = requestCounts.computeIfAbsent(clientIp,
        k -> new RateLimitEntry(now));

    // Reset window if expired
    if (now - entry.windowStart > WINDOW_MS) {
      entry.reset(now);
    }

    int count = entry.count.incrementAndGet();

    response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
    response.setHeader("X-RateLimit-Remaining",
        String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - count)));

    if (count > MAX_REQUESTS_PER_MINUTE) {
      log.warn("Rate limit exceeded for IP: {}", clientIp);
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"error\":\"Too many requests. Please try again later.\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    // Only rate-limit public endpoints
    return !path.startsWith("/api/v1/published-events");
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private void cleanup(long now) {
    requestCounts.entrySet().removeIf(
        e -> now - e.getValue().windowStart > WINDOW_MS * 2);
  }

  private static class RateLimitEntry {
    volatile long windowStart;
    final AtomicInteger count;

    RateLimitEntry(long windowStart) {
      this.windowStart = windowStart;
      this.count = new AtomicInteger(0);
    }

    void reset(long newWindowStart) {
      this.windowStart = newWindowStart;
      this.count.set(0);
    }
  }
}
