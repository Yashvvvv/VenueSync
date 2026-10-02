package com.fullstack.venuesync.staff;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fullstack.venuesync.shared.config.JwtAuthenticationConverter;
import com.fullstack.venuesync.shared.config.SecurityConfig;
import com.fullstack.venuesync.shared.domain.UserRepository;
import com.fullstack.venuesync.shared.exceptions.GlobalExceptionHandler;
import com.fullstack.venuesync.shared.filters.UserProvisioningFilter;
import com.fullstack.venuesync.staff.controller.StaffController;
import com.fullstack.venuesync.staff.dto.StaffInviteResponseDto;
import com.fullstack.venuesync.staff.exception.StaffInviteUsedException;
import com.fullstack.venuesync.staff.service.StaffInviteService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StaffController.class)
@Import({SecurityConfig.class, JwtAuthenticationConverter.class, GlobalExceptionHandler.class})
class StaffControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private StaffInviteService staffInviteService;
  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private UserProvisioningFilter userProvisioningFilter;
  @MockitoBean private UserRepository userRepository;

  private final UUID eventId = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    doAnswer(invocation -> {
      ((FilterChain) invocation.getArgument(2)).doFilter(
          (ServletRequest) invocation.getArgument(0), (ServletResponse) invocation.getArgument(1));
      return null;
    }).when(userProvisioningFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
  }

  private Jwt token() {
    return Jwt.withTokenValue("t").header("alg", "RS256").subject("auth0|someone").build();
  }

  @Test
  void organizerCreatesAnInvite() throws Exception {
    when(staffInviteService.createInvite(any(), eq(eventId)))
        .thenReturn(new StaffInviteResponseDto("K7Q2M-9XH4P", LocalDateTime.now().plusDays(7)));

    mockMvc.perform(post("/api/v1/events/{eventId}/staff-invites", eventId)
            .with(jwt().jwt(token()).authorities(new SimpleGrantedAuthority("ROLE_ORGANIZER"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("K7Q2M-9XH4P"));
  }

  @Test
  void anAttendeeCannotCreateInvites() throws Exception {
    mockMvc.perform(post("/api/v1/events/{eventId}/staff-invites", eventId)
            .with(jwt().jwt(token()).authorities(new SimpleGrantedAuthority("ROLE_ATTENDEE"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void anyoneSignedInCanRedeemAndAUsedCodeIs409() throws Exception {
    when(staffInviteService.acceptInvite(any(), eq("K7Q2M-9XH4P"))).thenThrow(new StaffInviteUsedException());

    mockMvc.perform(post("/api/v1/staff-invites/{code}/accept", "K7Q2M-9XH4P")
            .with(jwt().jwt(token()).authorities(new SimpleGrantedAuthority("ROLE_ATTENDEE"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVITE_USED"));
  }
}
