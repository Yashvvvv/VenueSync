package com.fullstack.venuesync.tickets.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
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
import com.fullstack.venuesync.tickets.dto.GetTicketResponseDto;
import com.fullstack.venuesync.tickets.dto.ListTicketResponseDto;
import com.fullstack.venuesync.tickets.mapper.TicketMapper;
import com.fullstack.venuesync.tickets.service.TicketService;
import com.fullstack.venuesync.validation.service.QrCodeService;

@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, JwtAuthenticationConverter.class, GlobalExceptionHandler.class})
class TicketControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TicketService ticketService;

  @MockitoBean
  private TicketMapper ticketMapper;

  @MockitoBean
  private QrCodeService qrCodeService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private UserProvisioningFilter userProvisioningFilter;

  @MockitoBean
  private UserRepository userRepository;

  private UUID userId;
  private UUID ticketId;
  private Ticket ticket;

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
    ticketId = UUID.randomUUID();

    ticket = new Ticket();
    ticket.setId(ticketId);
    ticket.setStatus(TicketStatusEnum.PURCHASED);
  }

  private Jwt createAttendeeJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(userId.toString())
        .claim("realm_access", java.util.Map.of("roles", List.of("ROLE_ATTENDEE")))
        .build();
  }

  @Nested
  @DisplayName("GET /api/v1/tickets")
  class ListTicketsEndpoint {

    @Test
    @DisplayName("should list tickets for attendee")
    void shouldListTicketsForAttendee() throws Exception {
      Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket));
      ListTicketResponseDto dto = new ListTicketResponseDto();

      when(ticketService.listTicketsForUser(any(UUID.class), any()))
          .thenReturn(ticketPage);
      when(ticketMapper.toListTicketResponseDto(any(Ticket.class))).thenReturn(dto);

      mockMvc.perform(get("/api/v1/tickets")
              .with(jwt().jwt(createAttendeeJwt()).authorities(
                  new SimpleGrantedAuthority("ROLE_ATTENDEE"))))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should list active tickets with filter")
    void shouldListActiveTicketsWithFilter() throws Exception {
      Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket));
      ListTicketResponseDto dto = new ListTicketResponseDto();

      when(ticketService.listActiveTicketsForUser(any(UUID.class), any()))
          .thenReturn(ticketPage);
      when(ticketMapper.toListTicketResponseDto(any(Ticket.class))).thenReturn(dto);

      mockMvc.perform(get("/api/v1/tickets")
              .with(jwt().jwt(createAttendeeJwt()).authorities(
                  new SimpleGrantedAuthority("ROLE_ATTENDEE")))
              .param("filter", "active"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should reject without ATTENDEE role")
    void shouldRejectWithoutAttendeeRole() throws Exception {
      mockMvc.perform(get("/api/v1/tickets")
              .with(jwt().jwt(createAttendeeJwt()).authorities(
                  new SimpleGrantedAuthority("ROLE_ORGANIZER"))))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/tickets/{id}")
  class GetTicketEndpoint {

    @Test
    @DisplayName("should get ticket for attendee")
    void shouldGetTicketForAttendee() throws Exception {
      GetTicketResponseDto dto = new GetTicketResponseDto();

      when(ticketService.getTicketForUser(any(UUID.class), eq(ticketId)))
          .thenReturn(Optional.of(ticket));
      when(ticketMapper.toGetTicketResponseDto(any(Ticket.class))).thenReturn(dto);

      mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId)
              .with(jwt().jwt(createAttendeeJwt()).authorities(
                  new SimpleGrantedAuthority("ROLE_ATTENDEE"))))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 404 when ticket not found")
    void shouldReturn404WhenNotFound() throws Exception {
      when(ticketService.getTicketForUser(any(UUID.class), eq(ticketId)))
          .thenReturn(Optional.empty());

      mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId)
              .with(jwt().jwt(createAttendeeJwt()).authorities(
                  new SimpleGrantedAuthority("ROLE_ATTENDEE"))))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/tickets/{id}/qr-codes")
  class GetQrCodeEndpoint {

    @Test
    @DisplayName("should return QR code image")
    void shouldReturnQrCodeImage() throws Exception {
      byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};

      when(qrCodeService.getQrCodeImageForUserAndTicket(any(UUID.class), eq(ticketId)))
          .thenReturn(imageBytes);

      mockMvc.perform(get("/api/v1/tickets/{ticketId}/qr-codes", ticketId)
              .with(jwt().jwt(createAttendeeJwt()).authorities(
                  new SimpleGrantedAuthority("ROLE_ATTENDEE"))))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
  }
}
