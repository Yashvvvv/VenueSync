package com.fullstack.venuesync.validation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullstack.venuesync.shared.config.SecurityConfig;
import com.fullstack.venuesync.shared.config.JwtAuthenticationConverter;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.GlobalExceptionHandler;
import com.fullstack.venuesync.shared.filters.UserProvisioningFilter;
import com.fullstack.venuesync.validation.domain.TicketValidation;
import com.fullstack.venuesync.validation.domain.TicketValidationMethod;
import com.fullstack.venuesync.validation.domain.TicketValidationStatusEnum;
import com.fullstack.venuesync.validation.dto.TicketValidationRequestDto;
import com.fullstack.venuesync.validation.dto.TicketValidationResponseDto;
import com.fullstack.venuesync.validation.mapper.TicketValidationMapper;
import com.fullstack.venuesync.validation.service.TicketValidationService;

@WebMvcTest(TicketValidationController.class)
@Import({SecurityConfig.class, JwtAuthenticationConverter.class, GlobalExceptionHandler.class})
class TicketValidationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private TicketValidationService ticketValidationService;

  @MockitoBean
  private TicketValidationMapper ticketValidationMapper;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private UserProvisioningFilter userProvisioningFilter;

  @MockitoBean
  private UserRepository userRepository;

  private UUID userId;
  private UUID ticketId;

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
  }

  private Jwt createStaffJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(userId.toString())
        .claim("realm_access", Map.of("roles", List.of("ROLE_STAFF")))
        .build();
  }

  @Test
  @DisplayName("should validate ticket with STAFF role via QR")
  void shouldValidateTicketWithStaffRole() throws Exception {
    TicketValidationRequestDto request = new TicketValidationRequestDto();
    request.setId(ticketId.toString());
    request.setMethod(TicketValidationMethod.QR_SCAN);

    TicketValidation validation = new TicketValidation();
    validation.setId(UUID.randomUUID());
    validation.setStatus(TicketValidationStatusEnum.VALID);

    TicketValidationResponseDto responseDto = new TicketValidationResponseDto();
    responseDto.setTicketId(ticketId);
    responseDto.setStatus(TicketValidationStatusEnum.VALID);

    when(ticketValidationService.validateTicketByQrCode(any(UUID.class)))
        .thenReturn(validation);
    when(ticketValidationMapper.toTicketValidationResponseDto(any(TicketValidation.class)))
        .thenReturn(responseDto);

    mockMvc.perform(post("/api/v1/ticket-validations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(jwt().jwt(createStaffJwt()).authorities(
                new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VALID"));
  }

  @Test
  @DisplayName("should reject validation without STAFF role")
  void shouldRejectValidationWithoutStaffRole() throws Exception {
    TicketValidationRequestDto request = new TicketValidationRequestDto();
    request.setId(ticketId.toString());
    request.setMethod(TicketValidationMethod.QR_SCAN);

    mockMvc.perform(post("/api/v1/ticket-validations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(jwt().jwt(createStaffJwt()).authorities(
                new SimpleGrantedAuthority("ROLE_ATTENDEE"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("should reject unauthenticated validation")
  void shouldRejectUnauthenticatedValidation() throws Exception {
    TicketValidationRequestDto request = new TicketValidationRequestDto();
    request.setId(ticketId.toString());
    request.setMethod(TicketValidationMethod.QR_SCAN);

    mockMvc.perform(post("/api/v1/ticket-validations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("should return 400 for missing id")
  void shouldReturn400ForMissingId() throws Exception {
    TicketValidationRequestDto request = new TicketValidationRequestDto();
    request.setMethod(TicketValidationMethod.QR_SCAN);

    mockMvc.perform(post("/api/v1/ticket-validations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(jwt().jwt(createStaffJwt()).authorities(
                new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("should return 400 for missing method")
  void shouldReturn400ForMissingMethod() throws Exception {
    String json = "{\"id\":\"" + ticketId + "\"}";

    mockMvc.perform(post("/api/v1/ticket-validations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .with(jwt().jwt(createStaffJwt()).authorities(
                new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isBadRequest());
  }
}
