package com.fullstack.venuesync.shared.config;

import java.util.Collection;
import java.util.Collections;
import com.fullstack.venuesync.shared.idp.Auth0Claims;
import java.util.List;
// LEGACY: java.util.Map was needed only by the Keycloak version of
// extractAuthorities below, which read a nested claim. Auth0's flat array does
// not need it. Left here, commented, so the legacy method reads as valid code.
// import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

  @Override
  public JwtAuthenticationToken convert(@NonNull Jwt jwt) {
    Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities);
  }

  /**
   * Auth0 emits a flat, namespaced array of roles:
   *
   * <pre>"https://venuesync.app/roles": ["ROLE_ATTENDEE"]</pre>
   *
   * <p>The namespace is not decoration. Auth0 silently discards custom claims
   * that are not URI-namespaced, so a claim called plain {@code roles} arrives
   * missing rather than empty - a valid token, no authorities, 403 everywhere,
   * nothing in the logs. This constant must match the namespace in the Auth0
   * Post-Login Action and ROLES_CLAIM in the frontend's use-roles.tsx.
   *
   * <p>The ROLE_ prefix filter is load-bearing: SecurityConfig uses
   * {@code hasRole("ATTENDEE")}, and Spring resolves that to an authority named
   * literally {@code ROLE_ATTENDEE}. Roles must therefore be named with the
   * prefix in the Auth0 dashboard too.
   */
  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList(Auth0Claims.ROLES);

    if (null == roles) {
      return Collections.emptyList();
    }

    return roles.stream()
        .filter(role -> role.startsWith("ROLE_"))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // LEGACY: the Keycloak version of the same method (kept for reference, see
  // keycloak/README.md and the commented block in application.properties).
  //
  // The only difference between the two IDPs, on this side of the wire, is the
  // shape of one claim. Keycloak nests roles one level deep:
  //
  //     "realm_access": { "roles": ["ROLE_ATTENDEE"] }
  //
  // which needs an unchecked cast out of a Map, plus a containsKey guard.
  // Auth0's flat array is read directly by getClaimAsStringList, so the cast
  // and the @SuppressWarnings go away. Everything else in this class - the
  // ROLE_ filter, SimpleGrantedAuthority, the Converter contract - is identical,
  // because neither IDP changes what a resource server does with a valid token.
  //
  // private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
  //   Map<String, Object> realmAccess = jwt.getClaim("realm_access");
  //
  //   if(null == realmAccess || !realmAccess.containsKey("roles")) {
  //     return Collections.emptyList();
  //   }
  //
  //   @SuppressWarnings("unchecked")
  //   List<String> roles = (List<String>)realmAccess.get("roles");
  //
  //   return roles.stream()
  //       .filter(role -> role.startsWith("ROLE_"))
  //       .map(SimpleGrantedAuthority::new)
  //       .collect(Collectors.toList());
  // }
  // -------------------------------------------------------------------------
}
