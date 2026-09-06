package com.fullstack.venuesync.shared.idp;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;
import com.fullstack.venuesync.shared.keycloak.KeycloakAdminService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Assigns Auth0 roles to users via the Management API - the ATTENDEE to
 * ORGANIZER upgrade bridge.
 *
 * <p>This is the one place the app talks to the IDP in the <i>opposite</i>
 * direction from everything else. Normally the backend is a resource server: it
 * receives a token and validates it, and never calls the IDP at all. Here it
 * acts as a client, logging in as itself (client credentials - no human) to
 * write a role back into the directory.
 *
 * <p>It implements {@link KeycloakAdminService} deliberately. The interface is
 * a port with one method and no Keycloak types in its signature, so keeping it
 * means UserServiceImpl and the controller need no changes at all - the whole
 * IDP swap stops at this file. The name is now historical; see
 * KeycloakAdminServiceImpl for the implementation it replaced.
 *
 * <p>Only two endpoints are needed, so this uses Spring's own RestClient rather
 * than adding the Auth0 SDK as a dependency.
 */
@Slf4j
@Service
public class Auth0RoleService implements KeycloakAdminService {

  private final RestClient client;
  private final String clientId;
  private final String clientSecret;
  private final String managementAudience;

  /**
   * Reads the environment variables directly rather than going through
   * {@code auth0.management.*} keys in application.properties.
   *
   * <p>That indirection did not work: with the properties defined, and
   * AUTH0_M2M_CLIENT_ID confirmed present in the container, Spring still failed
   * with "Could not resolve placeholder 'auth0.management.client-id'" - even
   * after giving the key an empty default, which should be unfailable. Supplying
   * AUTH0_MANAGEMENT_CLIENT_ID as an environment variable booted fine, proving
   * the Environment simply never received the key from the file. Rather than
   * ship a mapping that demonstrably breaks in a container (and would have
   * broken identically on Render), the service reads the variables it needs.
   *
   * <p>The two M2M values default to empty because they are needed only by the
   * ORGANIZER upgrade; AUTH0_DOMAIN has no default so a misconfigured deployment
   * still fails at startup rather than at the first token validation.
   */
  public Auth0RoleService(
      @Value("${AUTH0_DOMAIN}") String domain,
      @Value("${AUTH0_M2M_CLIENT_ID:}") String clientId,
      @Value("${AUTH0_M2M_CLIENT_SECRET:}") String clientSecret) {
    this.client = RestClient.create("https://" + domain);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.managementAudience = "https://" + domain + "/api/v2/";
  }

  /**
   * @param userId the raw Auth0 subject ("auth0|68b3..."), not the derived local
   *     users.id UUID. UserController reads it straight off the JWT.
   */
  @Override
  public void assignRoleToUser(String userId, String roleName) {
    if (clientId.isBlank() || clientSecret.isBlank()) {
      throw new VenueSyncException(
          "Auth0 Management credentials are not configured - set AUTH0_M2M_CLIENT_ID and "
              + "AUTH0_M2M_CLIENT_SECRET. Role upgrades are disabled until then.");
    }
    log.info("Assigning role {} to user {} in Auth0", roleName, userId);
    try {
      String token = fetchManagementToken();

      client.post()
          .uri("/api/v2/users/{id}/roles", userId)
          .header("Authorization", "Bearer " + token)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("roles", List.of(findRoleId(token, roleName))))
          .retrieve()
          .toBodilessEntity();

      log.info("Successfully assigned role {} to user {}", roleName, userId);

    } catch (VenueSyncException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to assign role to user in Auth0", e);
      throw new VenueSyncException("Failed to update user roles", e);
    }
  }

  /**
   * Client-credentials grant: the app authenticating as itself, with no user
   * involved. This is what the M2M application in the Auth0 dashboard is for.
   *
   * <p>ponytail: fetches a fresh token on every call. Role upgrades are a
   * once-per-user action, so the extra round trip is free. Cache it (they last
   * 24h) if this ever runs hot.
   */
  private String fetchManagementToken() {
    TokenResponse response = client.post()
        .uri("/oauth/token")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of(
            "grant_type", "client_credentials",
            "client_id", clientId,
            "client_secret", clientSecret,
            "audience", managementAudience))
        .retrieve()
        .body(TokenResponse.class);

    if (response == null || response.access_token() == null) {
      throw new VenueSyncException("Identity provider did not return a management token");
    }
    return response.access_token();
  }

  /**
   * Auth0 addresses roles by an opaque id, not by name, so the name has to be
   * resolved first. Keycloak could fetch a role by name directly, which is why
   * the legacy implementation had no equivalent of this method.
   */
  private String findRoleId(String token, String roleName) {
    // name_filter is a partial match, so the exact name still has to be checked.
    Role[] roles = client.get()
        .uri(uri -> uri.path("/api/v2/roles").queryParam("name_filter", roleName).build())
        .header("Authorization", "Bearer " + token)
        .retrieve()
        .body(Role[].class);

    return Arrays.stream(roles == null ? new Role[0] : roles)
        .filter(role -> roleName.equals(role.name()))
        .findFirst()
        .orElseThrow(
            () -> new VenueSyncException("Role not found in identity provider: " + roleName))
        .id();
  }

  private record TokenResponse(String access_token) {}

  private record Role(String id, String name) {}
}
