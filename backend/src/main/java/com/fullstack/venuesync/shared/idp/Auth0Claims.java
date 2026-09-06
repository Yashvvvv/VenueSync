package com.fullstack.venuesync.shared.idp;

/**
 * The custom claims the Auth0 Post-Login Action injects into the access token.
 *
 * <p>Auth0 silently discards custom claims that are not URI-namespaced, so these
 * must stay namespaced and must match the {@code namespace} constant in the
 * Action exactly. A mismatch is fail-closed and quiet: the token is valid, the
 * claims are simply absent, and every request 403s with nothing in the logs.
 *
 * <p>{@link #ROLES} must also match {@code ROLES_CLAIM} in the frontend's
 * use-roles.tsx, which decodes the same token for UI gating.
 *
 * <p>Keycloak needed no equivalent to this class: it put roles under the
 * standard {@code realm_access.roles} and {@code email} / {@code preferred_username}
 * directly on the access token, so claim names were hard-coded at each use site.
 * Auth0 puts email and name on the ID token only, which is why they are
 * re-injected here as namespaced access-token claims.
 */
public final class Auth0Claims {

  public static final String NAMESPACE = "https://venuesync.app";
  public static final String ROLES = NAMESPACE + "/roles";
  public static final String EMAIL = NAMESPACE + "/email";
  public static final String NAME = NAMESPACE + "/name";

  private Auth0Claims() {}
}
