package com.fullstack.venuesync.shared.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

  @Test
  void shouldReturnUpStatus() {
    HealthController controller = new HealthController();

    ResponseEntity<Map<String, String>> response = controller.health();

    assertEquals(200, response.getStatusCode().value());
    assertEquals("UP", response.getBody().get("status"));
    assertEquals("venuesync", response.getBody().get("service"));
  }
}