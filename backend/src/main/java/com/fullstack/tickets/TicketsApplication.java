package com.fullstack.tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TicketsApplication {

  public static void main(String[] args) {
    // Set timezone to match where events are happening (wall clock time approach)
    // This ensures LocalDateTime.now() returns the expected local time
    // Change this to match your deployment region
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    System.setProperty("user.timezone", "Asia/Kolkata");
    
    SpringApplication.run(TicketsApplication.class, args);
  }

}
