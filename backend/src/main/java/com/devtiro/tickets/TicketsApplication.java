package com.devtiro.tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TicketsApplication {

  public static void main(String[] args) {
    // Ensure Java reports a Postgres-accepted timezone id
    System.setProperty("user.timezone", "Asia/Kolkata");
    SpringApplication.run(TicketsApplication.class, args);
  }

}
