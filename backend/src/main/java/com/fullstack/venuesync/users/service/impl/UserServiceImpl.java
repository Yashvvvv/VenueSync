package com.fullstack.venuesync.users.service.impl;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;
import com.fullstack.venuesync.shared.keycloak.KeycloakAdminService;
import com.fullstack.venuesync.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final KeycloakAdminService keycloakAdminService;

    @Override
    public void upgradeUserToOrganizer(String userId, String email) {
        log.info("Upgrade Request Received | User: {} | Email: {} | Timestamp: {}", userId, email, Instant.now());
        
        // Ensure user is not already an organizer in the current security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER"))) {
            log.warn("User {} is already an ORGANIZER. Rejecting upgrade request.", userId);
            throw new VenueSyncException("User is already an Organizer");
        }

        try {
            keycloakAdminService.assignRoleToUser(userId, "ROLE_ORGANIZER");
            log.info("Upgrade Successful | User: {} | Email: {} | Upgraded from ATTENDEE to ORGANIZER", userId, email);
        } catch (VenueSyncException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upgrade user {} to ORGANIZER", userId, e);
            throw new VenueSyncException("Failed to upgrade user account", e);
        }
    }
}
