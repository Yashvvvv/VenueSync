package com.fullstack.venuesync.users.controller;

import com.fullstack.venuesync.shared.idp.Auth0Claims;
import com.fullstack.venuesync.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/me/roles/organizer")
    public ResponseEntity<Void> upgradeToOrganizer(@AuthenticationPrincipal Jwt jwt) {
        // Raw IDP subject ("auth0|68b3..."), NOT the derived users.id UUID -
        // the Management API addresses users by their own identifier.
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString(Auth0Claims.EMAIL);

        // LEGACY: Keycloak put email on the access token under its standard
        // name. Auth0 keeps it on the ID token, so the Post-Login Action
        // re-injects it namespaced. Used for logging only.
        // String email = jwt.getClaimAsString("email");


        userService.upgradeUserToOrganizer(userId, email);
        
        return ResponseEntity.ok().build();
    }
}
