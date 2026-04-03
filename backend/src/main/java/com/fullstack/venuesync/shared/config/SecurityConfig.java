package com.fullstack.venuesync.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fullstack.venuesync.shared.filters.UserProvisioningFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

  @Value("${app.cors.allowed-origins:http://localhost:5173}")
  private String allowedOrigins;

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      UserProvisioningFilter userProvisioningFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
    http
        .authorizeHttpRequests(authorize ->
            authorize
                .requestMatchers(HttpMethod.GET, "/api/v1/published-events/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers("/health").permitAll()
                .requestMatchers("/api/v1/events").hasRole("ORGANIZER")
                .requestMatchers("/api/v1/ticket-validations").hasRole("STAFF")
                .requestMatchers("/api/v1/tickets/**").hasRole("ATTENDEE")
                .requestMatchers("/api/v1/events/*/ticket-types/*/tickets").hasRole("ATTENDEE")
                // Catch-all rule
                .anyRequest().authenticated())
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(oauth2 ->
            oauth2.jwt(jwt ->
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
            ))
        .addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

}
