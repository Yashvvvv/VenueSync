package com.fullstack.venuesync.shared.keycloak;

// ===========================================================================
// LEGACY - self-hosted Keycloak implementation of KeycloakAdminService.
// Commented out rather than deleted; see keycloak/README.md.
//
// Superseded by shared/idp/Auth0RoleService, which implements the same
// interface. The interface itself is still live and still called by
// UserServiceImpl - only the implementation changed.
//
// Why it is commented and not merely unused: it imports org.keycloak.*, so it
// cannot compile once keycloak-admin-client is removed from pom.xml (that
// dependency is commented out there for the same reason). Uncommenting this
// file means uncommenting the dependency and the keycloak.admin.* properties
// in application.properties too - all three move together.
//
// How the two implementations differ:
//
//   auth       Keycloak logged in as a HUMAN admin (username + password against
//              the master realm). Auth0 uses client credentials - an M2M app
//              authenticating as itself. No human password anywhere.
//   roles      Keycloak could fetch a role by name in one call. Auth0 addresses
//              roles by opaque id, so Auth0RoleService resolves name -> id first.
//   transport  Keycloak needed a whole SDK (keycloak-admin-client, ~15 transitive
//              deps) to build a typed client. Auth0 needs two REST calls, so
//              Spring's own RestClient covers it with no new dependency.
//
// What did NOT change: the method signature, the VenueSyncException wrapping,
// the log lines, and the fact that this is the only outbound call to the IDP
// in the entire application.
// ===========================================================================
//
// import com.fullstack.venuesync.shared.exceptions.VenueSyncException;
// import java.util.Collections;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.keycloak.admin.client.Keycloak;
// import org.keycloak.admin.client.resource.RealmResource;
// import org.keycloak.admin.client.resource.UserResource;
// import org.keycloak.representations.idm.RoleRepresentation;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Service;
//
// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class KeycloakAdminServiceImpl implements KeycloakAdminService {
//
//     private final Keycloak keycloak;
//
//     @Value("${keycloak.admin.realm}")
//     private String realm;
//
//     @Override
//     public void assignRoleToUser(String userId, String roleName) {
//         log.info("Assigning role {} to user {} in Keycloak", roleName, userId);
//         try {
//             RealmResource realmResource = keycloak.realm(realm);
//             UserResource userResource = realmResource.users().get(userId);
//
//             // Fetch the role from Keycloak
//             RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
//             if (role == null) {
//                 log.error("Role {} not found in realm {}", roleName, realm);
//                 throw new VenueSyncException("Role not found in identity provider");
//             }
//
//             // Assign the role
//             userResource.roles().realmLevel().add(Collections.singletonList(role));
//             log.info("Successfully assigned role {} to user {}", roleName, userId);
//
//         } catch (Exception e) {
//             log.error("Failed to assign role to user in Keycloak", e);
//             throw new VenueSyncException("Failed to update user roles", e);
//         }
//     }
// }
