package com.fullstack.venuesync.tickets.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.fullstack.venuesync.events.domain.SalesStatus;
import com.fullstack.venuesync.events.exception.SalesPeriodException;
import com.fullstack.venuesync.tickets.dto.GetTicketResponseDto;
import com.fullstack.venuesync.tickets.exception.TicketTypeNotFoundException;
import com.fullstack.venuesync.tickets.exception.TicketsSoldOutException;
import com.fullstack.venuesync.tickets.mapper.TicketMapper;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(TicketTypeController.class)
@Import({SecurityConfig.class, JwtAuthenticationConverter.class, GlobalExceptionHandler.class})
class TicketTypeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TicketTypeService ticketTypeService;

  @MockitoBean
  private TicketMapper ticketMapper;

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

  private ResultActions purchaseAsAttendee(String idempotencyKey) throws Exception {
    var request = post("/api/v1/events/{eventId}/ticket-types/{ticketTypeId}/tickets", eventId, ticketTypeId)
        .with(jwt().jwt(createAttendeeJwt()).authorities(new SimpleGrantedAuthority("ROLE_ATTENDEE")));
    if (idempotencyKey != null) {
      request.header("Idempotency-Key", idempotencyKey);
    }
    return mockMvc.perform(request);
  }

  @Test
  @DisplayName("should create the ticket and return 201 with Location and body")
  void shouldPurchaseTicketWithAttendeeRole() throws Exception {
    UUID key = UUID.randomUUID();
    Ticket ticket = new Ticket();
    ticket.setId(UUID.randomUUID());
    ticket.setStatus(TicketStatusEnum.PURCHASED);
    GetTicketResponseDto dto = new GetTicketResponseDto();
    dto.setId(ticket.getId());
    dto.setTicketTypeName("General");

    when(ticketTypeService.purchaseTicket(any(UUID.class), eq(eventId), eq(ticketTypeId), eq(key)))
        .thenReturn(ticket);
    when(ticketMapper.toGetTicketResponseDto(ticket)).thenReturn(dto);

    purchaseAsAttendee(key.toString())
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/tickets/" + ticket.getId()))
        .andExpect(jsonPath("$.id").value(ticket.getId().toString()))
        .andExpect(jsonPath("$.ticketTypeName").value("General"));
  }

  @Test
  @DisplayName("should reject a purchase without an Idempotency-Key with 400, not 500")
  void shouldRejectMissingIdempotencyKey() throws Exception {
    purchaseAsAttendee(null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("should reject a malformed Idempotency-Key with 400, not 500")
  void shouldRejectMalformedIdempotencyKey() throws Exception {
    purchaseAsAttendee("not-a-uuid")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("should answer sold out with 409 TICKETS_SOLD_OUT")
  void shouldMapSoldOut() throws Exception {
    when(ticketTypeService.purchaseTicket(any(), any(), any(), any())).thenThrow(new TicketsSoldOutException());

    purchaseAsAttendee(UUID.randomUUID().toString())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TICKETS_SOLD_OUT"));
  }

  @Test
  @DisplayName("should answer a closed sales window with 409 and the matching code")
  void shouldMapSalesPeriod() throws Exception {
    when(ticketTypeService.purchaseTicket(any(), any(), any(), any()))
        .thenThrow(new SalesPeriodException(SalesStatus.UPCOMING))
        .thenThrow(new SalesPeriodException(SalesStatus.ENDED));

    purchaseAsAttendee(UUID.randomUUID().toString())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SALES_NOT_STARTED"));
    purchaseAsAttendee(UUID.randomUUID().toString())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SALES_ENDED"));
  }

  @Test
  @DisplayName("should answer a ticket type outside this event with 404")
  void shouldMapTicketTypeNotFound() throws Exception {
    when(ticketTypeService.purchaseTicket(any(), any(), any(), any()))
        .thenThrow(new TicketTypeNotFoundException("not in event"));

    purchaseAsAttendee(UUID.randomUUID().toString())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TICKET_TYPE_NOT_FOUND"));
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
