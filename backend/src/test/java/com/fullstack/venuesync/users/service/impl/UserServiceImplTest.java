package com.fullstack.venuesync.users.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fullstack.venuesync.shared.exceptions.VenueSyncException;
import com.fullstack.venuesync.shared.keycloak.KeycloakAdminService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class UserServiceImplTest {

  private final KeycloakAdminService roles = mock(KeycloakAdminService.class);
  private final UserServiceImpl service = new UserServiceImpl(roles);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void upgradeKeepsAttendeeAndAddsOrganizer() {
    service.upgradeUserToOrganizer("auth0|abc", "a@example.com");

    InOrder order = inOrder(roles);
    order.verify(roles).assignRoleToUser("auth0|abc", "ROLE_ATTENDEE");
    order.verify(roles).assignRoleToUser("auth0|abc", "ROLE_ORGANIZER");
  }

  @Test
  void organizerIsNotAssignedWhenAttendeeFails() {
    doThrow(new VenueSyncException("Failed to update user roles"))
        .when(roles).assignRoleToUser(anyString(), eq("ROLE_ATTENDEE"));

    assertThatThrownBy(() -> service.upgradeUserToOrganizer("auth0|abc", "a@example.com"))
        .isInstanceOf(VenueSyncException.class);
    verify(roles, never()).assignRoleToUser(anyString(), eq("ROLE_ORGANIZER"));
  }

  @Test
  void anExistingOrganizerIsRejected() {
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "auth0|abc", null, List.of(new SimpleGrantedAuthority("ROLE_ORGANIZER"))));

    assertThatThrownBy(() -> service.upgradeUserToOrganizer("auth0|abc", "a@example.com"))
        .isInstanceOf(VenueSyncException.class);
    verify(roles, never()).assignRoleToUser(anyString(), anyString());
  }
}
