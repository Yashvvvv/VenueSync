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
            // Roles add capabilities, they never replace them: an organizer still buys and holds tickets. ATTENDEE is
            // assigned explicitly because the Post-Login Action's default only applies to an account with no roles at
            // all, so assigning ORGANIZER alone silently took ATTENDEE away. ATTENDEE goes first: if the second call
            // fails, the account is still a working attendee. Auth0 treats re-assigning a held role as a no-op.
            // ponytail: two Management API tokens per upgrade (one per call); one batched call if upgrades get common.
            keycloakAdminService.assignRoleToUser(userId, "ROLE_ATTENDEE");
            keycloakAdminService.assignRoleToUser(userId, "ROLE_ORGANIZER");
            log.info("Upgrade Successful | User: {} | Email: {} | Now ATTENDEE and ORGANIZER", userId, email);
        } catch (VenueSyncException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upgrade user {} to ORGANIZER", userId, e);
            throw new VenueSyncException("Failed to upgrade user account", e);
        }
    }
}

