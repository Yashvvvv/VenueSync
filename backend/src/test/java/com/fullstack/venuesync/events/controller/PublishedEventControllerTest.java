package com.fullstack.venuesync.events.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.dto.GetPublishedEventDetailsResponseDto;
import com.fullstack.venuesync.events.dto.ListPublishedEventResponseDto;
import com.fullstack.venuesync.events.mapper.EventMapper;
import com.fullstack.venuesync.events.service.EventService;
import com.fullstack.venuesync.shared.config.SecurityConfig;
import com.fullstack.venuesync.shared.config.JwtAuthenticationConverter;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.GlobalExceptionHandler;
import com.fullstack.venuesync.shared.filters.UserProvisioningFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(PublishedEventController.class)
@Import({SecurityConfig.class, JwtAuthenticationConverter.class, GlobalExceptionHandler.class})
class PublishedEventControllerTest {

  @Autowired
  private MockMvc mockMvc;

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

    eventId = UUID.randomUUID();
    event = new Event();
    event.setId(eventId);
    event.setName("Published Event");
    event.setVenue("Venue");
    event.setStatus(EventStatusEnum.PUBLISHED);
    event.setTicketTypes(new ArrayList<>());
  }

  @Test
  @DisplayName("should list published events without authentication")
  void shouldListPublishedEventsWithoutAuth() throws Exception {
    Page<Event> eventPage = new PageImpl<>(List.of(event));
    ListPublishedEventResponseDto dto = new ListPublishedEventResponseDto();

    when(eventService.listPublishedEvents(any())).thenReturn(eventPage);
    when(eventMapper.toListPublishedEventResponseDto(any(Event.class))).thenReturn(dto);

    mockMvc.perform(get("/api/v1/published-events")
            .param("page", "0")
            .param("size", "4"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("should search published events")
  void shouldSearchPublishedEvents() throws Exception {
    Page<Event> eventPage = new PageImpl<>(List.of(event));
    ListPublishedEventResponseDto dto = new ListPublishedEventResponseDto();

    when(eventService.searchPublishedEvents(eq("test"), any())).thenReturn(eventPage);
    when(eventMapper.toListPublishedEventResponseDto(any(Event.class))).thenReturn(dto);

    mockMvc.perform(get("/api/v1/published-events")
            .param("q", "test"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("should get published event details")
  void shouldGetPublishedEventDetails() throws Exception {
    GetPublishedEventDetailsResponseDto dto = new GetPublishedEventDetailsResponseDto();

    when(eventService.getPublishedEvent(eventId)).thenReturn(Optional.of(event));
    when(eventMapper.toGetPublishedEventDetailsResponseDto(any(Event.class))).thenReturn(dto);

    mockMvc.perform(get("/api/v1/published-events/{eventId}", eventId))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("should return 404 for non-existent published event")
  void shouldReturn404ForNonExistentEvent() throws Exception {
    when(eventService.getPublishedEvent(eventId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/v1/published-events/{eventId}", eventId))
        .andExpect(status().isNotFound());
  }
}
