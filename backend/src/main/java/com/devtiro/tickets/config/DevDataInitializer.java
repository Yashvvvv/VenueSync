package com.devtiro.tickets.config;

import com.devtiro.tickets.domain.entities.Event;
import com.devtiro.tickets.domain.entities.EventStatusEnum;
import com.devtiro.tickets.domain.entities.TicketType;
import com.devtiro.tickets.domain.entities.User;
import com.devtiro.tickets.repositories.EventRepository;
import com.devtiro.tickets.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create a test user
        User testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setName("Test Organizer");
        testUser.setEmail("test@example.com");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(testUser);

        // Create a test event
        Event testEvent = Event.builder()
                .name("Test Event")
                .venue("Test Venue")
                .start(LocalDateTime.now().plusDays(7))
                .end(LocalDateTime.now().plusDays(7).plusHours(3))
                .salesStart(LocalDateTime.now())
                .salesEnd(LocalDateTime.now().plusDays(6))
                .status(EventStatusEnum.PUBLISHED)
                .organizer(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        eventRepository.save(testEvent);

        // Create a test ticket type
        TicketType testTicketType = TicketType.builder()
                .name("General Admission")
                .price(25.0)
                .description("General admission ticket")
                .totalAvailable(100)
                .event(testEvent)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testEvent.getTicketTypes().add(testTicketType);
        eventRepository.save(testEvent);
    }
}
