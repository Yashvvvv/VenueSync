package com.fullstack.tickets.config;

import com.fullstack.tickets.domain.entities.Event;
import com.fullstack.tickets.domain.entities.EventStatusEnum;
import com.fullstack.tickets.domain.entities.TicketType;
import com.fullstack.tickets.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data loader that seeds the database with sample events on startup.
 * Adds sample events that don't already exist (checks by name).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final EventRepository eventRepository;

    @Override
    public void run(String... args) {
        log.info("Checking for sample events to add...");
        List<Event> sampleEvents = createSampleEvents();
        
        // Get existing event names to avoid duplicates
        List<String> existingNames = eventRepository.findAll().stream()
                .map(Event::getName)
                .toList();
        
        // Filter out events that already exist
        List<Event> newEvents = sampleEvents.stream()
                .filter(event -> !existingNames.contains(event.getName()))
                .toList();
        
        if (newEvents.isEmpty()) {
            log.info("All sample events already exist, skipping.");
            return;
        }
        
        eventRepository.saveAll(newEvents);
        log.info("Successfully added {} new sample events.", newEvents.size());
    }

    private List<Event> createSampleEvents() {
        List<Event> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. Tech Conference
        Event techConf = Event.builder()
                .name("TechCon 2025 - AI & Innovation Summit")
                .venue("Silicon Valley Convention Center, San Francisco, CA")
                .start(now.plusDays(30).withHour(9).withMinute(0))
                .end(now.plusDays(32).withHour(18).withMinute(0))
                .salesStart(now.minusDays(30))
                .salesEnd(now.plusDays(28))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(techConf, 
            new TicketTypeInfo("Early Bird", 299.99, "Limited early bird pricing - includes all sessions", 100),
            new TicketTypeInfo("General Admission", 499.99, "Full access to all conference sessions and workshops", 500),
            new TicketTypeInfo("VIP Pass", 999.99, "VIP seating, exclusive networking dinner, and swag bag", 50)
        );
        events.add(techConf);

        // 2. Music Festival
        Event musicFest = Event.builder()
                .name("Summer Vibes Music Festival")
                .venue("Central Park Amphitheater, New York, NY")
                .start(now.plusDays(45).withHour(14).withMinute(0))
                .end(now.plusDays(45).withHour(23).withMinute(0))
                .salesStart(now.minusDays(15))
                .salesEnd(now.plusDays(44))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(musicFest,
            new TicketTypeInfo("General Admission", 79.99, "Standing area access", 2000),
            new TicketTypeInfo("Premium", 149.99, "Reserved seating with great views", 500),
            new TicketTypeInfo("VIP Experience", 299.99, "Front row access, meet & greet, complimentary drinks", 100)
        );
        events.add(musicFest);

        // 3. Comedy Night
        Event comedyNight = Event.builder()
                .name("Stand-Up Comedy Night with Top Comedians")
                .venue("The Laugh Factory, Los Angeles, CA")
                .start(now.plusDays(14).withHour(20).withMinute(0))
                .end(now.plusDays(14).withHour(23).withMinute(0))
                .salesStart(now.minusDays(7))
                .salesEnd(now.plusDays(13))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(comedyNight,
            new TicketTypeInfo("Standard Seat", 35.00, "Great seats for a night of laughs", 200),
            new TicketTypeInfo("Front Row", 65.00, "Best seats in the house - be prepared to be part of the show!", 30)
        );
        events.add(comedyNight);

        // 4. Art Exhibition
        Event artExhibition = Event.builder()
                .name("Modern Art Gala - Contemporary Masters")
                .venue("Metropolitan Art Gallery, Chicago, IL")
                .start(now.plusDays(21).withHour(18).withMinute(0))
                .end(now.plusDays(21).withHour(22).withMinute(0))
                .salesStart(now.minusDays(14))
                .salesEnd(now.plusDays(20))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(artExhibition,
            new TicketTypeInfo("General Entry", 45.00, "Access to all exhibition halls", 300),
            new TicketTypeInfo("Guided Tour", 75.00, "Includes expert-led tour of the exhibition", 50),
            new TicketTypeInfo("Patron Package", 250.00, "Private viewing, champagne reception, artist meet & greet", 25)
        );
        events.add(artExhibition);

        // 5. Sports Event
        Event sportsEvent = Event.builder()
                .name("Championship Finals - Basketball Showdown")
                .venue("Madison Square Garden, New York, NY")
                .start(now.plusDays(60).withHour(19).withMinute(30))
                .end(now.plusDays(60).withHour(22).withMinute(30))
                .salesStart(now)
                .salesEnd(now.plusDays(59))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(sportsEvent,
            new TicketTypeInfo("Upper Level", 89.99, "Great view of the entire court", 1000),
            new TicketTypeInfo("Lower Level", 199.99, "Closer to the action", 500),
            new TicketTypeInfo("Courtside", 599.99, "Experience the game up close", 50)
        );
        events.add(sportsEvent);

        // 6. Food & Wine Festival
        Event foodFest = Event.builder()
                .name("Gourmet Food & Wine Festival")
                .venue("Napa Valley Vineyards, California")
                .start(now.plusDays(25).withHour(11).withMinute(0))
                .end(now.plusDays(25).withHour(20).withMinute(0))
                .salesStart(now.minusDays(20))
                .salesEnd(now.plusDays(24))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(foodFest,
            new TicketTypeInfo("Tasting Pass", 125.00, "10 tasting tickets, souvenir glass", 400),
            new TicketTypeInfo("Connoisseur", 225.00, "Unlimited tastings, chef demos, recipe book", 150),
            new TicketTypeInfo("Grand Cru", 450.00, "All access + private vineyard tour + dinner", 40)
        );
        events.add(foodFest);

        // 7. Startup Pitch Night
        Event startupPitch = Event.builder()
                .name("Startup Pitch Night - Shark Tank Style")
                .venue("Innovation Hub, Austin, TX")
                .start(now.plusDays(10).withHour(18).withMinute(0))
                .end(now.plusDays(10).withHour(21).withMinute(30))
                .salesStart(now.minusDays(5))
                .salesEnd(now.plusDays(9))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(startupPitch,
            new TicketTypeInfo("Observer", 25.00, "Watch the pitches and networking", 200),
            new TicketTypeInfo("Investor Circle", 150.00, "Priority seating, post-event mixer with founders", 50)
        );
        events.add(startupPitch);

        // 8. Yoga Retreat
        Event yogaRetreat = Event.builder()
                .name("Sunrise Yoga & Wellness Retreat")
                .venue("Serenity Gardens, Sedona, AZ")
                .start(now.plusDays(35).withHour(6).withMinute(0))
                .end(now.plusDays(37).withHour(12).withMinute(0))
                .salesStart(now.minusDays(10))
                .salesEnd(now.plusDays(30))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(yogaRetreat,
            new TicketTypeInfo("Day Pass", 85.00, "Single day access to all sessions", 100),
            new TicketTypeInfo("Full Retreat", 350.00, "3-day full experience with meals included", 50),
            new TicketTypeInfo("Private Sessions", 550.00, "Includes 1-on-1 sessions with master instructors", 15)
        );
        events.add(yogaRetreat);

        // 9. Gaming Tournament
        Event gamingTourney = Event.builder()
                .name("eSports Championship - League Finals")
                .venue("Esports Arena, Las Vegas, NV")
                .start(now.plusDays(40).withHour(12).withMinute(0))
                .end(now.plusDays(40).withHour(22).withMinute(0))
                .salesStart(now)
                .salesEnd(now.plusDays(39))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(gamingTourney,
            new TicketTypeInfo("Spectator", 45.00, "Watch the action live", 800),
            new TicketTypeInfo("Gamer Lounge", 95.00, "Access to gaming stations and swag", 200),
            new TicketTypeInfo("All-Access", 175.00, "Backstage tour, player autographs, exclusive merch", 75)
        );
        events.add(gamingTourney);

        // 10. Jazz Night
        Event jazzNight = Event.builder()
                .name("Jazz Under the Stars")
                .venue("Blue Note Jazz Club, New Orleans, LA")
                .start(now.plusDays(7).withHour(20).withMinute(0))
                .end(now.plusDays(7).withHour(23).withMinute(30))
                .salesStart(now.minusDays(14))
                .salesEnd(now.plusDays(6))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(jazzNight,
            new TicketTypeInfo("Standing", 40.00, "Standing room with bar access", 150),
            new TicketTypeInfo("Table Seating", 75.00, "Reserved table for two", 40),
            new TicketTypeInfo("VIP Booth", 200.00, "Private booth, bottle service included", 10)
        );
        events.add(jazzNight);

        // 11. Workshop - Photography
        Event photoWorkshop = Event.builder()
                .name("Master Photography Workshop")
                .venue("Creative Studios, Seattle, WA")
                .start(now.plusDays(18).withHour(10).withMinute(0))
                .end(now.plusDays(18).withHour(17).withMinute(0))
                .salesStart(now.minusDays(7))
                .salesEnd(now.plusDays(17))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(photoWorkshop,
            new TicketTypeInfo("Workshop Ticket", 189.00, "Full day workshop, includes materials and lunch", 30)
        );
        events.add(photoWorkshop);

        // 12. Book Signing
        Event bookSigning = Event.builder()
                .name("Bestselling Author Book Signing & Talk")
                .venue("Barnes & Noble Union Square, New York, NY")
                .start(now.plusDays(5).withHour(18).withMinute(0))
                .end(now.plusDays(5).withHour(20).withMinute(0))
                .salesStart(now.minusDays(10))
                .salesEnd(now.plusDays(4))
                .status(EventStatusEnum.PUBLISHED)
                .build();
        addTicketTypes(bookSigning,
            new TicketTypeInfo("Free Entry", 0.00, "Free admission - book purchase encouraged", 100),
            new TicketTypeInfo("Book Bundle", 35.00, "Includes signed copy of the new book", 75)
        );
        events.add(bookSigning);

        return events;
    }

    private void addTicketTypes(Event event, TicketTypeInfo... ticketTypeInfos) {
        for (TicketTypeInfo info : ticketTypeInfos) {
            TicketType ticketType = TicketType.builder()
                    .name(info.name)
                    .price(info.price)
                    .description(info.description)
                    .totalAvailable(info.totalAvailable)
                    .event(event)
                    .build();
            event.getTicketTypes().add(ticketType);
        }
    }

    private record TicketTypeInfo(String name, double price, String description, int totalAvailable) {}
}
