package com.fullstack.venuesync.shared.config;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.repository.EventRepository;
import com.fullstack.venuesync.events.service.EventImageService;
import com.fullstack.venuesync.tickets.domain.TicketType;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The demo catalogue: twelve events across India, each with INR prices and a photo (from Unsplash, free to use; see
 * resources/demo-events/CREDITS.md).
 *
 * <p>Runs on every start and only ever converges: an event that's missing is created; one still carrying its old
 * US demo name is rewritten in place (same id, so tickets already bought for it stay valid); one without a photo gets
 * its photo. A demo event is found by its exact name and venue together (the old US ones by theirs), which no
 * organizer's event shares, so nothing an organizer made is touched.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

  private final EventRepository eventRepository;
  private final EventImageService imageService;
  private final TransactionTemplate transactions;

  @Override
  public void run(String... args) {
    int created = 0;
    int rewritten = 0;
    for (Demo demo : DEMOS) {
      try {
        // One transaction per event: the photo's bulk update clears the persistence context, and a failure in one
        // demo event must never stop the app from starting.
        Outcome outcome = transactions.execute(status -> converge(demo));
        if (outcome == Outcome.CREATED) created++;
        if (outcome == Outcome.REWRITTEN) rewritten++;
      } catch (RuntimeException e) {
        log.warn("Demo event '{}' skipped: {}", demo.name(), e.getMessage());
      }
    }
    log.info("Demo catalogue: {} created, {} rewritten for India.", created, rewritten);
  }

  private enum Outcome { CREATED, REWRITTEN, UNCHANGED }

  private Outcome converge(Demo demo) {
    LocalDateTime now = LocalDateTime.now();
    Optional<Event> current = eventRepository.findFirstByNameAndVenue(demo.name(), demo.venue());
    Outcome outcome = Outcome.UNCHANGED;
    Event event;
    if (current.isPresent()) {
      event = current.get();
    } else {
      Optional<Event> legacy = eventRepository.findFirstByNameAndVenue(demo.legacyName(), demo.legacyVenue());
      event = legacy.orElseGet(Event::new);
      outcome = legacy.isPresent() ? Outcome.REWRITTEN : Outcome.CREATED;
      apply(demo, event, now, legacy.isEmpty());
      event = eventRepository.saveAndFlush(event);
    }
    if (event.getImageUpdatedAt() == null) {
      imageService.store(event.getId(), photo(demo.photo()));
    }
    return outcome;
  }

  /** Writes the demo's Indian details onto [event], keeping anything a buyer already depends on. */
  private static void apply(Demo demo, Event event, LocalDateTime now, boolean fresh) {
    event.setName(demo.name());
    event.setVenue(demo.venue());
    // ponytail: a rewritten demo event whose dates have passed is moved into the future so the catalogue looks
    // alive; a real organizer's event is never rewritten.
    boolean over = event.getEnd() != null && event.getEnd().isBefore(now);
    if (fresh || over || event.getStart() == null) {
      event.setStart(now.plusDays(demo.startDay()).withHour(demo.startHour()).withMinute(demo.startMinute()).withSecond(0).withNano(0));
      event.setEnd(now.plusDays(demo.endDay()).withHour(demo.endHour()).withMinute(demo.endMinute()).withSecond(0).withNano(0));
      event.setSalesStart(now.plusDays(demo.salesStartDay()).withSecond(0).withNano(0));
      event.setSalesEnd(event.getStart().minusHours(1));
      event.setStatus(EventStatusEnum.PUBLISHED);
    }
    for (Tier tier : demo.tiers()) {
      TicketType type = event.getTicketTypes().stream()
          .filter(t -> t.getName().equals(tier.legacyName()) || t.getName().equals(tier.name()))
          .findFirst()
          .orElseGet(() -> {
            TicketType added = new TicketType();
            added.setEvent(event);
            event.getTicketTypes().add(added);
            return added;
          });
      type.setName(tier.name());
      type.setPrice(tier.price());
      type.setDescription(tier.description());
      // Never below what an earlier capacity allowed: tickets may already be sold against it.
      type.setTotalAvailable(Math.max(tier.total(), type.getTotalAvailable() == null ? 0 : type.getTotalAvailable()));
    }
  }

  private static byte[] photo(String file) {
    try (InputStream in = new ClassPathResource("demo-events/" + file + ".jpg").getInputStream()) {
      return in.readAllBytes();
    } catch (IOException e) {
      throw new IllegalStateException("Demo photo missing: " + file, e);
    }
  }

  /** Day offsets are from the first start; times are wall clock (ADR-003). */
  private record Demo(String name, String legacyName, String venue, String legacyVenue, String photo,
      int startDay, int startHour, int startMinute, int endDay, int endHour, int endMinute, int salesStartDay,
      List<Tier> tiers) {}

  /** [legacyName] is the US demo tier this one replaces, so a rewrite updates it instead of adding another. */
  private record Tier(String legacyName, String name, double price, String description, int total) {}

  static final List<Demo> DEMOS = List.of(
      new Demo("Bengaluru AI & Innovation Summit", "TechCon 2025 - AI & Innovation Summit",
          "Bangalore International Exhibition Centre, Bengaluru", "Silicon Valley Convention Center, San Francisco, CA",
          "ai-summit", 30, 9, 30, 32, 18, 0, -30, List.of(
          new Tier("Early Bird", "Early Bird", 2499, "Limited early-bird pricing, every session included", 100),
          new Tier("General Admission", "General Admission", 4999, "All talks, workshops and the expo floor", 500),
          new Tier("VIP Pass", "VIP Pass", 9999, "Front seating, the founders' dinner and a swag kit", 50))),
      new Demo("Monsoon Beats Music Festival", "Summer Vibes Music Festival",
          "Mahalaxmi Racecourse, Mumbai", "Central Park Amphitheater, New York, NY",
          "music-festival", 45, 16, 0, 45, 23, 0, -15, List.of(
          new Tier("General Admission", "General Admission", 1499, "Standing access to the main arena", 2000),
          new Tier("Premium", "Gold", 2999, "Raised viewing deck close to the stage", 500),
          new Tier("VIP Experience", "Fan Pit", 5999, "Front-of-stage pit, express entry and a drinks counter", 100))),
      new Demo("Stand-Up Comedy Night: Mumbai Open", "Stand-Up Comedy Night with Top Comedians",
          "The Habitat, Khar West, Mumbai", "The Laugh Factory, Los Angeles, CA",
          "comedy", 14, 20, 0, 14, 22, 30, -7, List.of(
          new Tier("Standard Seat", "Standard Seat", 599, "Good seats for a night of laughs", 200),
          new Tier("Front Row", "Front Row", 999, "Best seats in the house; expect to become part of the set", 30))),
      new Demo("Contemporary Indian Art Showcase", "Modern Art Gala - Contemporary Masters",
          "India Habitat Centre, Lodhi Road, New Delhi", "Metropolitan Art Gallery, Chicago, IL",
          "art", 21, 11, 0, 21, 19, 0, -14, List.of(
          new Tier("General Entry", "General Entry", 300, "Access to every gallery in the show", 300),
          new Tier("Guided Tour", "Guided Walkthrough", 600, "An hour-long walk through the show with the curator", 50),
          new Tier("Patron Package", "Patron Evening", 2500, "Private viewing, high tea and a meet with the artists", 25))),
      new Demo("Kabaddi League Finals", "Championship Finals - Basketball Showdown",
          "Thyagaraj Sports Complex, New Delhi", "Madison Square Garden, New York, NY",
          "kabaddi", 60, 19, 30, 60, 22, 30, 0, List.of(
          new Tier("Upper Level", "General Stand", 399, "A full view of the mat", 1000),
          new Tier("Lower Level", "Premium Stand", 999, "Closer to the raids and tackles", 500),
          new Tier("Courtside", "Mat Side", 2999, "Seats right at the edge of the mat", 50))),
      new Demo("Goa Food & Music Carnival", "Gourmet Food & Wine Festival",
          "Campal Grounds, Panaji, Goa", "Napa Valley Vineyards, California",
          "food-carnival", 25, 12, 0, 25, 22, 0, -20, List.of(
          new Tier("Tasting Pass", "Tasting Pass", 799, "Ten tasting coupons and a souvenir mug", 400),
          new Tier("Connoisseur", "Food Lover", 1499, "Unlimited tastings, chef demos and a recipe booklet", 150),
          new Tier("Grand Cru", "Chef's Table", 3999, "All access plus a seated Goan dinner with the chefs", 40))),
      new Demo("Startup Pitch Night: Founders & Funders", "Startup Pitch Night - Shark Tank Style",
          "Innovation Hub, Koramangala, Bengaluru", "Innovation Hub, Austin, TX",
          "startup", 10, 18, 30, 10, 21, 30, -5, List.of(
          new Tier("Observer", "Observer", 299, "Watch the pitches and stay for the networking", 200),
          new Tier("Investor Circle", "Investor Circle", 1999, "Front seating and the after-mixer with the founders", 50))),
      new Demo("Sunrise Yoga Retreat on the Ganga", "Sunrise Yoga & Wellness Retreat",
          "Ganga Ghat, Rishikesh, Uttarakhand", "Serenity Gardens, Sedona, AZ",
          "yoga", 35, 6, 0, 37, 12, 0, -10, List.of(
          new Tier("Day Pass", "Day Pass", 999, "One day of riverside sessions", 100),
          new Tier("Full Retreat", "Full Retreat", 7499, "Three days with sattvic meals and a riverside stay", 50),
          new Tier("Private Sessions", "Private Sessions", 11999, "The full retreat plus one-on-one sessions with the teachers", 15))),
      new Demo("Campus Esports Championship Finals", "eSports Championship - League Finals",
          "NSCI Dome, Worli, Mumbai", "Esports Arena, Las Vegas, NV",
          "esports", 40, 12, 0, 40, 22, 0, 0, List.of(
          new Tier("Spectator", "Spectator", 499, "Watch the finals live in the arena", 800),
          new Tier("Gamer Lounge", "Gamer Lounge", 999, "Play stations, freeplay and a merch pack", 200),
          new Tier("All-Access", "All-Access", 2499, "Backstage tour, player signings and exclusive merch", 75))),
      new Demo("Jazz Nights at Bandra Fort", "Jazz Under the Stars",
          "Bandra Fort Amphitheatre, Mumbai", "Blue Note Jazz Club, New Orleans, LA",
          "jazz", 7, 19, 30, 7, 22, 30, -14, List.of(
          new Tier("Standing", "Standing", 699, "Open lawn with bar access", 150),
          new Tier("Table Seating", "Table for Two", 1999, "A reserved table for two near the stage", 40),
          new Tier("VIP Booth", "Lounge Booth", 4999, "Private booth for four with dinner included", 10))),
      new Demo("Street Photography Walk: Old Delhi", "Master Photography Workshop",
          "Chandni Chowk, Old Delhi", "Creative Studios, Seattle, WA",
          "old-delhi", 18, 7, 0, 18, 12, 0, -7, List.of(
          new Tier("Workshop Ticket", "Walk & Workshop", 1499, "A guided walk, a review session and chai", 30))),
      new Demo("Meet the Author: Reading & Book Signing", "Bestselling Author Book Signing & Talk",
          "Bahrisons Booksellers, Khan Market, New Delhi", "Barnes & Noble Union Square, New York, NY",
          "books", 5, 18, 0, 5, 20, 0, -10, List.of(
          new Tier("Free Entry", "Free Entry", 0, "Free to attend; buying a book is up to you", 100),
          new Tier("Book Bundle", "Signed Copy Bundle", 599, "A signed copy of the new book, set aside for you", 75))));
}
