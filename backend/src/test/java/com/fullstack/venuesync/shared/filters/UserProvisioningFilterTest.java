package com.fullstack.venuesync.shared.filters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class UserProvisioningFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private FilterChain filterChain;
  @InjectMocks private UserProvisioningFilter filter;

  private HttpServletRequest request;
  private HttpServletResponse response;

  @BeforeEach
  void authenticateAsAuth0User() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();

    // Auth0 subjects are NOT UUIDs; that shape is the point of the test.
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("sub", "google-oauth2|109031341950465598791")
        .build();

    // The authorities-taking constructor is required: JwtAuthenticationToken(jwt)
    // alone leaves isAuthenticated() false, and the filter would skip its body.
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, Collections.emptyList()));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("a concurrent insert of the same user does not fail the request")
  void survivesProvisioningRace() throws Exception {
    // Both requests see no row, both insert, this one loses.
    when(userRepository.existsById(any(UUID.class))).thenReturn(false);
    when(userRepository.save(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key value violates users_pkey"));

    assertDoesNotThrow(() -> filter.doFilter(request, response, filterChain),
        "losing the provisioning race must not surface as a 500");

    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  @DisplayName("an already-provisioned user is not inserted again")
  void doesNotReinsertExistingUser() throws Exception {
    when(userRepository.existsById(any(UUID.class))).thenReturn(true);

    filter.doFilter(request, response, filterChain);

    verify(userRepository, never()).save(any(User.class));
    verify(filterChain, times(1)).doFilter(request, response);
  }
}
