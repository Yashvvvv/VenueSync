package com.fullstack.venuesync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VenueSyncApplication {

  public static void main(String[] args) {
    // Time zone comes from app.timezone via TimezoneConfig (ADR-003) — not hard-coded here.
    SpringApplication.run(VenueSyncApplication.class, args);
  }

}
