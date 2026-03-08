package com.fullstack.venuesync.tickets.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fullstack.venuesync.shared.config.SecurityConfig;
import com.fullstack.venuesync.shared.config.JwtAuthenticationConverter;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.GlobalExceptionHandler;
import com.fullstack.venuesync.shared.filters.UserProvisioningFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;
import com.fullstack.venuesync.tickets.service.TicketTypeService;

@WebMvcTest(TicketTypeController.class)
@Import({SecurityConfig.class, JwtAuthenticationConverter.class, GlobalExceptionHandler.class})
class TicketTypeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TicketTypeService ticketTypeService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private UserProvisioningFilter userProvisioningFilter;

  @MockitoBean
  private UserRepository userRepository;

  private UUID userId;
  private UUID eventId;
  private UUID ticketTypeId;

  @BeforeEach
  void setUp() throws Exception {
    // Configure mocked filter to pass through the filter chain
    doAnswer(invocation -> {
      ((FilterChain) invocation.getArgument(2)).doFilter(
          (ServletRequest) invocation.getArgument(0),
          (ServletResponse) invocation.getArgument(1));
      return null;
    }).when(userProvisioningFilter).doFilter(
        any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

    userId = UUID.randomUUID();
    eventId = UUID.randomUUID();
    ticketTypeId = UUID.randomUUID();
  }

  private Jwt createAttendeeJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(userId.toString())
        .claim("realm_access", java.util.Map.of("roles", List.of("ROLE_ATTENDEE")))
        .build();
  }

  @Test
  @DisplayName("should purchase ticket with ATTENDEE role")
  void shouldPurchaseTicketWithAttendeeRole() throws Exception {
    Ticket ticket = new Ticket();
    ticket.setId(UUID.randomUUID());
    ticket.setStatus(TicketStatusEnum.PURCHASED);

    when(ticketTypeService.purchaseTicket(any(UUID.class), eq(ticketTypeId)))
        .thenReturn(ticket);

    mockMvc.perform(post("/api/v1/events/{eventId}/ticket-types/{ticketTypeId}/tickets",
            eventId, ticketTypeId)
            .with(jwt().jwt(createAttendeeJwt()).authorities(
                new SimpleGrantedAuthority("ROLE_ATTENDEE"))))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("should reject purchase without ATTENDEE role")
  void shouldRejectPurchaseWithoutAttendeeRole() throws Exception {
    mockMvc.perform(post("/api/v1/events/{eventId}/ticket-types/{ticketTypeId}/tickets",
            eventId, ticketTypeId)
            .with(jwt().jwt(createAttendeeJwt()).authorities(
                new SimpleGrantedAuthority("ROLE_ORGANIZER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("should reject unauthenticated purchase")
  void shouldRejectUnauthenticatedPurchase() throws Exception {
    mockMvc.perform(post("/api/v1/events/{eventId}/ticket-types/{ticketTypeId}/tickets",
            eventId, ticketTypeId))
        .andExpect(status().isUnauthorized());
  }
}
