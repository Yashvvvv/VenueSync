package com.fullstack.venuesync.shared.filters;

import com.fullstack.venuesync.shared.idp.Auth0Claims;
import com.fullstack.venuesync.shared.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProvisioningFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof Jwt jwt) {

      // Must be the same derivation the controllers use, or a user is inserted
      // under one id and then looked up under another.
      UUID userId = JwtUtil.parseUserId(jwt);

      if (!userRepository.existsById(Objects.requireNonNull(userId))) {

        User user = new User();
        user.setId(userId);
        user.setName(jwt.getClaimAsString(Auth0Claims.NAME));
        user.setEmail(jwt.getClaimAsString(Auth0Claims.EMAIL));

        try {
          userRepository.save(user);
        } catch (DataIntegrityViolationException raced) {
          // existsById + save is check-then-act, and the frontend fires several
          // API calls in parallel on the first page after login. On a brand-new
          // user they all reach this filter at once, all see no row, and all
          // insert - one wins, the rest got a 500 on users_pkey.
          //
          // The intent here is "ensure this user exists", which the winner has
          // already satisfied, so losing the race is success, not failure.
          // Swallowing it is what makes the filter idempotent; a lock would not
          // help anyway once more than one instance is running.
          log.debug("User {} was provisioned concurrently by another request", userId);
        }
      }

      // LEGACY: the Keycloak version of the three lines above.
      //
      // Keycloak's `sub` was itself a UUID, so it could be parsed straight into
      // the primary key. Auth0's is not ("auth0|68b3...", "google-oauth2|1043...")
      // and UUID.fromString throws IllegalArgumentException on it - a 500, not a
      // 403, which is why this was the first thing to break after the swap.
      //
      // Keycloak also put these two claims on the access token under standard
      // names. Auth0 keeps email and name on the ID token, so the Post-Login
      // Action re-injects them as namespaced access-token claims instead.
      //
      // UUID keycloakId = UUID.fromString(jwt.getSubject());
      //
      // if (!userRepository.existsById(Objects.requireNonNull(keycloakId))) {
      //
      //   User user = new User();
      //   user.setId(keycloakId);
      //   user.setName(jwt.getClaimAsString("preferred_username"));
      //   user.setEmail(jwt.getClaimAsString("email"));
      //
      //   userRepository.save(user);
      // }
    }

    filterChain.doFilter(request, response);
  }

}
