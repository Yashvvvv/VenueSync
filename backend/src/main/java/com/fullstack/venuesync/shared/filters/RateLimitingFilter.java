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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Simple in-memory rate limiter for public API endpoints.
 * Limits each IP to a configurable number of requests per minute.
 * Uses a fixed one-minute window per client.
 *
 * <p>This is suitable for free-tier single-instance deployments.
 * For multi-instance deployments, use Redis-backed rate limiting.</p>
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS_PER_MINUTE = 60;
  private static final long WINDOW_MS = 60_000L;
  /** IPv4 or IPv6 text, nothing longer: a key is never an 8 KB header a caller chose. */
  private static final Pattern IP = Pattern.compile("[0-9A-Fa-f:.]{2,45}");
  static final int MAX_CLIENTS = 10_000;

  /**
   * Least-recently-used, capped at MAX_CLIENTS. A full map evicts the longest-idle client, which only hands that client
   * a fresh quota. It used to let every new client through unlimited instead, so ~10k addresses switched limiting off.
   * A flood can't evict a client that's still sending, so nobody gets more than their own 60 a minute.
   */
  // ponytail: one lock around the map; stripe it or move to Redis if it ever shows up in a profile.
  private final Map<String, RateLimitEntry> requestCounts = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, RateLimitEntry> eldest) {
      return size() > MAX_CLIENTS;
    }
  };

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String clientIp = getClientIp(request);
    long now = System.currentTimeMillis();

    int count;
    synchronized (requestCounts) {
      RateLimitEntry entry = requestCounts.computeIfAbsent(clientIp, k -> new RateLimitEntry(now));
      if (now - entry.windowStart > WINDOW_MS) {
        entry.windowStart = now;
        entry.count = 0;
      }
      count = ++entry.count;
    }

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

  /**
   * Who the quota belongs to. In production Cloudflare sits in front of Render and sets CF-Connecting-IP to the real
   * client (it refuses requests that bring their own, error 1000), so that one can't be chosen by the caller. The
   * left-most X-Forwarded-For entry CAN be: Cloudflare puts a caller's own value in front of the real address, which
   * gave every request a fresh quota. Only kept as the fallback for running without Cloudflare (local, Docker).
   */
  private String getClientIp(HttpServletRequest request) {
    String cloudflare = request.getHeader("CF-Connecting-IP");
    if (cloudflare != null && IP.matcher(cloudflare.trim()).matches()) {
      return cloudflare.trim();
    }
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null) {
      String first = xForwardedFor.split(",")[0].trim();
      if (IP.matcher(first).matches()) {
        return first;
      }
    }
    return request.getRemoteAddr();
  }

  /** Only touched while holding the {@code requestCounts} lock. */
  private static class RateLimitEntry {
    long windowStart;
    int count;

    RateLimitEntry(long windowStart) {
      this.windowStart = windowStart;
    }
  }
}
