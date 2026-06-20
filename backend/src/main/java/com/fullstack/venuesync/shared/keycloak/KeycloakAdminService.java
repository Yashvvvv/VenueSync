package com.fullstack.venuesync.shared.keycloak;

public interface KeycloakAdminService {
    void assignRoleToUser(String userId, String roleName);
}
