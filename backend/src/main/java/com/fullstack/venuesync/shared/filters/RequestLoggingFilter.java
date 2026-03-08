package com.fullstack.venuesync.shared.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that logs incoming HTTP requests and outgoing responses with timing information.
 * Adds a correlation ID header (X-Request-Id) for request tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString().substring(0, 8);
    }

    response.setHeader(REQUEST_ID_HEADER, requestId);

    long startTime = System.currentTimeMillis();
    String method = request.getMethod();
    String uri = request.getRequestURI();

    log.info("[{}] --> {} {}", requestId, method, uri);

    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      log.info("[{}] <-- {} {} {} ({}ms)", requestId, method, uri,
          response.getStatus(), duration);
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    // Skip logging for actuator endpoints to reduce noise
    return path.startsWith("/actuator");
  }
}
