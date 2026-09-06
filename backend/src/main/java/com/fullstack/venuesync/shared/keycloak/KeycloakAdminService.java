package com.fullstack.venuesync.shared.keycloak;

/**
 * Port for "assign a role to a user in the identity provider".
 *
 * <p>Still live, still called by UserServiceImpl. The name is historical: the
 * implementation is now {@link com.fullstack.venuesync.shared.idp.Auth0RoleService},
 * and {@link KeycloakAdminServiceImpl} is commented out beside it.
 *
 * <p>The interface is kept because it has no Keycloak types in its signature -
 * two Strings in, nothing out. That is precisely why swapping the IDP touched
 * one class instead of rippling into UserServiceImpl and UserController.
 */
public interface KeycloakAdminService {
    void assignRoleToUser(String userId, String roleName);
}
