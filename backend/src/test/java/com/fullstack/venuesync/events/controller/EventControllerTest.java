package com.fullstack.venuesync.events.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullstack.venuesync.events.domain.CreateEventRequest;
import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.domain.UpdateEventRequest;
import com.fullstack.venuesync.events.dto.*;
import com.fullstack.venuesync.events.mapper.EventMapper;
import com.fullstack.venuesync.events.service.EventService;
import com.fullstack.venuesync.shared.config.SecurityConfig;
import com.fullstack.venuesync.shared.config.JwtAuthenticationConverter;
import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.GlobalExceptionHandler;
import com.fullstack.venuesync.shared.filters.UserProvisioningFilter;
import com.fullstack.venuesync.tickets.domain.TicketType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtAuthenticationConverter.class, GlobalExceptionHandler.class})
class EventControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private EventService eventService;

  @MockitoBean
  private EventMapper eventMapper;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private UserProvisioningFilter userProvisioningFilter;

  @MockitoBean
  private UserRepository userRepository;

  private UUID userId;
  private UUID eventId;
  private Event event;

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

    event = new Event();
    event.setId(eventId);
    event.setName("Test Event");
    event.setVenue("Test Venue");
    event.setStatus(EventStatusEnum.DRAFT);
    event.setTicketTypes(new ArrayList<>());
  }

  private Jwt createJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(userId.toString())
        .claim("realm_access", java.util.Map.of("roles", List.of("ROLE_ORGANIZER")))
        .build();
  }

  @Nested
  @DisplayName("POST /api/v1/events")
  class CreateEventEndpoint {

    @Test
    @DisplayName("should create event with ORGANIZER role")
    void shouldCreateEventWithOrganizerRole() throws Exception {
      CreateEventResponseDto responseDto = new CreateEventResponseDto();

      when(eventMapper.fromDto(any(CreateEventRequestDto.class)))
          .thenReturn(new CreateEventRequest());
      when(eventService.createEvent(any(UUID.class), any(CreateEventRequest.class)))
          .thenReturn(event);
      when(eventMapper.toDto(any(Event.class))).thenReturn(responseDto);

      String requestJson = """
          {
            "name": "Test Event",
            "venue": "Test Venue",
            "status": "DRAFT",
            "ticketTypes": [{"name": "General", "price": 50.0}]
          }
          """;

      mockMvc.perform(post("/api/v1/events")
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ORGANIZER")))
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("should reject without ORGANIZER role")
    void shouldRejectWithoutOrganizerRole() throws Exception {
      String requestJson = """
          {
            "name": "Test Event",
            "venue": "Test Venue",
            "status": "DRAFT",
            "ticketTypes": [{"name": "General", "price": 50.0}]
          }
          """;

      mockMvc.perform(post("/api/v1/events")
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ATTENDEE")))
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should reject unauthenticated request")
    void shouldRejectUnauthenticatedRequest() throws Exception {
      String requestJson = """
          {
            "name": "Test Event",
            "venue": "Test Venue",
            "status": "DRAFT",
            "ticketTypes": [{"name": "General", "price": 50.0}]
          }
          """;

      mockMvc.perform(post("/api/v1/events")
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 400 for invalid request body")
    void shouldReturn400ForInvalidRequestBody() throws Exception {
      String requestJson = """
          {
            "name": "",
            "venue": "",
            "ticketTypes": []
          }
          """;

      mockMvc.perform(post("/api/v1/events")
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ORGANIZER")))
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/events")
  class ListEventsEndpoint {

    @Test
    @DisplayName("should list events for organizer")
    void shouldListEventsForOrganizer() throws Exception {
      Page<Event> eventPage = new PageImpl<>(List.of(event));
      ListEventResponseDto dto = new ListEventResponseDto();

      when(eventService.listEventsForOrganizer(any(UUID.class), any()))
          .thenReturn(eventPage);
      when(eventMapper.toListEventResponseDto(any(Event.class))).thenReturn(dto);

      mockMvc.perform(get("/api/v1/events")
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ORGANIZER")))
              .param("page", "0")
              .param("size", "6"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should list events filtered by status")
    void shouldListEventsFilteredByStatus() throws Exception {
      Page<Event> eventPage = new PageImpl<>(List.of(event));
      ListEventResponseDto dto = new ListEventResponseDto();

      when(eventService.listEventsForOrganizerByStatus(any(UUID.class), eq(EventStatusEnum.DRAFT), any()))
          .thenReturn(eventPage);
      when(eventMapper.toListEventResponseDto(any(Event.class))).thenReturn(dto);

      mockMvc.perform(get("/api/v1/events")
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ORGANIZER")))
              .param("status", "DRAFT"))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/events/{id}")
  class GetEventEndpoint {

    @Test
    @DisplayName("should get event for organizer")
    void shouldGetEventForOrganizer() throws Exception {
      GetEventDetailsResponseDto dto = new GetEventDetailsResponseDto();

      when(eventService.getEventForOrganizer(any(UUID.class), eq(eventId)))
          .thenReturn(Optional.of(event));
      when(eventMapper.toGetEventDetailsResponseDto(any(Event.class))).thenReturn(dto);

      mockMvc.perform(get("/api/v1/events/{eventId}", eventId)
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ORGANIZER"))))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 404 when event not found")
    void shouldReturn404WhenNotFound() throws Exception {
      when(eventService.getEventForOrganizer(any(UUID.class), eq(eventId)))
          .thenReturn(Optional.empty());

      mockMvc.perform(get("/api/v1/events/{eventId}", eventId)
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ORGANIZER"))))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/events/{id}")
  class DeleteEventEndpoint {

    @Test
    @DisplayName("should delete event successfully")
    void shouldDeleteEventSuccessfully() throws Exception {
      doNothing().when(eventService).deleteEventForOrganizer(any(UUID.class), eq(eventId));

      mockMvc.perform(delete("/api/v1/events/{eventId}", eventId)
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ORGANIZER"))))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/events/counts")
  class EventCountsEndpoint {

    @Test
    @DisplayName("should return event counts")
    void shouldReturnEventCounts() throws Exception {
      when(eventService.countEventsForOrganizerByStatus(any(UUID.class), any(EventStatusEnum.class)))
          .thenReturn(5L);

      mockMvc.perform(get("/api/v1/events/counts")
              .with(jwt().jwt(createJwt()).authorities(
                  new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ORGANIZER"))))
          .andExpect(status().isOk());
    }
  }
}
