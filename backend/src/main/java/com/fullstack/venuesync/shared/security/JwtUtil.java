package com.fullstack.venuesync.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtUtil {
  private JwtUtil(){
  }

  /**
   * Derives the local {@code users.id} from the IDP subject.
   *
   * <p>Every controller that needs "who is calling" routes through here, and so
   * does UserProvisioningFilter when it inserts the row - they must agree, or a
   * user gets provisioned under one id and looked up under another.
   *
   * <p>Keycloak's {@code sub} was itself a UUID, so this was
   * {@code UUID.fromString(jwt.getSubject())}. Auth0's is not
   * ("auth0|68b3...", "google-oauth2|1090...") and that call throws
   * IllegalArgumentException - a 500 on every authenticated request, after the
   * token has already validated successfully. Hashing the subject into a
   * deterministic name-based UUID keeps {@code users.id} a UUID and leaves every
   * foreign key to it untouched, which parsing or widening the column would not.
   *
   * <p>ponytail: derived id, not the raw subject. Store the raw {@code sub} in
   * its own column if anything ever needs to map a local user back to the IDP
   * without a token in hand. Nothing does today - UserController reads the raw
   * subject straight off the JWT for the Management API call.
   */
  public static UUID parseUserId(Jwt jwt) {
    return UUID.nameUUIDFromBytes(jwt.getSubject().getBytes(StandardCharsets.UTF_8));
  }
}
