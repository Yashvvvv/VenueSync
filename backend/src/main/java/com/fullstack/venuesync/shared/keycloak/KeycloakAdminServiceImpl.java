package com.fullstack.venuesync.shared.keycloak;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private final Keycloak keycloak;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Override
    public void assignRoleToUser(String userId, String roleName) {
        log.info("Assigning role {} to user {} in Keycloak", roleName, userId);
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            // Fetch the role from Keycloak
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            if (role == null) {
                log.error("Role {} not found in realm {}", roleName, realm);
                throw new VenueSyncException("Role not found in identity provider");
            }

            // Assign the role
            userResource.roles().realmLevel().add(Collections.singletonList(role));
            log.info("Successfully assigned role {} to user {}", roleName, userId);
            
        } catch (Exception e) {
            log.error("Failed to assign role to user in Keycloak", e);
            throw new VenueSyncException("Failed to update user roles", e);
        }
    }
}
