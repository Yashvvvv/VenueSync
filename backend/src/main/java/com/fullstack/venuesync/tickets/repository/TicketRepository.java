package com.fullstack.venuesync.tickets.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fullstack.venuesync.tickets.domain.Ticket;
import com.fullstack.venuesync.tickets.domain.TicketStatusEnum;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  int countByTicketTypeId(UUID ticketTypeId);

  Optional<Ticket> findByPurchaserIdAndIdempotencyKey(UUID purchaserId, UUID idempotencyKey);

  /** Rows of [ticketTypeId, ticketsSold] for every ticket type of the event that has sold at least one. */
  @Query("SELECT t.ticketType.id, COUNT(t) FROM Ticket t " +
         "WHERE t.ticketType.event.id = :eventId GROUP BY t.ticketType.id")
  List<Object[]> countSoldByTicketTypeForEvent(@Param("eventId") UUID eventId);

  /** Rows of [ticketTypeId, ticketsIssued] across several events at once: one query for a page of an organizer's events. */
  @Query("SELECT t.ticketType.id, COUNT(t) FROM Ticket t " +
         "WHERE t.ticketType.event.id IN :eventIds GROUP BY t.ticketType.id")
  List<Object[]> countSoldByTicketTypeForEvents(@Param("eventIds") java.util.Collection<UUID> eventIds);

  Page<Ticket> findByPurchaserId(UUID purchaserId, Pageable pageable);

  Optional<Ticket> findByIdAndPurchaserId(UUID id, UUID purchaserId);

  /**
   * Find all active (PURCHASED) tickets for a user, ordered by event start date.
   * Active tickets are those with status PURCHASED where the event has not ended yet.
   */
  @Query("SELECT t FROM Ticket t " +
         "JOIN FETCH t.ticketType tt " +
         "JOIN FETCH tt.event e " +
         "WHERE t.purchaser.id = :purchaserId " +
         "AND t.status = :status " +
         "AND (e.end IS NULL OR e.end > :now) " +
         "ORDER BY e.start ASC")
  Page<Ticket> findActiveTicketsByPurchaserId(
      @Param("purchaserId") UUID purchaserId,
      @Param("status") TicketStatusEnum status,
      @Param("now") LocalDateTime now,
      Pageable pageable
  );

  /**
   * Find all past tickets for a user (USED, EXPIRED, or events that have ended).
   */
  @Query("SELECT t FROM Ticket t " +
         "JOIN FETCH t.ticketType tt " +
         "JOIN FETCH tt.event e " +
         "WHERE t.purchaser.id = :purchaserId " +
         "AND (t.status IN :pastStatuses OR (e.end IS NOT NULL AND e.end <= :now)) " +
         "ORDER BY e.start DESC")
  Page<Ticket> findPastTicketsByPurchaserId(
      @Param("purchaserId") UUID purchaserId,
      @Param("pastStatuses") java.util.List<TicketStatusEnum> pastStatuses,
      @Param("now") LocalDateTime now,
      Pageable pageable
  );

  /** Tickets of one event whose id starts with a ticket code's hex prefix (callers pass a limit of 2 to spot clashes). */
  @Query("SELECT t FROM Ticket t WHERE t.ticketType.event.id = :eventId AND CAST(t.id AS String) LIKE CONCAT(:prefix, '%')")
  List<Ticket> findByEventAndCodePrefix(
      @Param("eventId") UUID eventId, @Param("prefix") String prefix, Pageable pageable);

  /** The door guest list: an event's tickets whose attendee name or email contains [pattern], or whose code starts with [codePrefix]. */
  @Query("SELECT t FROM Ticket t JOIN FETCH t.purchaser u JOIN FETCH t.ticketType tt "
      + "WHERE tt.event.id = :eventId AND (LOWER(u.name) LIKE :pattern OR LOWER(u.email) LIKE :pattern "
      + "OR CAST(t.id AS String) LIKE :codePrefix) ORDER BY u.name")
  List<Ticket> searchGuests(
      @Param("eventId") UUID eventId,
      @Param("pattern") String pattern,
      @Param("codePrefix") String codePrefix,
      Pageable pageable);

  /**
   * Admits a ticket: PURCHASED -> USED as ONE statement, so of two simultaneous scans exactly one sees 1
   * (and is VALID) and the other sees 0. A read-then-write would let both read PURCHASED and both admit.
   *
   * @return 1 if this call admitted the ticket, 0 if it was not PURCHASED (anymore)
   */
  @Modifying
  @Query("UPDATE Ticket t SET t.status = :used, t.updatedAt = :now WHERE t.id = :id AND t.status = :purchased")
  int markUsed(
      @Param("id") UUID id,
      @Param("purchased") TicketStatusEnum purchased,
      @Param("used") TicketStatusEnum used,
      @Param("now") LocalDateTime now
  );

  /**
   * Update tickets to EXPIRED status for events that have ended.
   * Only updates tickets that are currently PURCHASED.
   */
  @Modifying
  @Query("UPDATE Ticket t SET t.status = :newStatus, t.updatedAt = :now " +
         "WHERE t.status = :currentStatus " +
         "AND t.ticketType.event.end IS NOT NULL " +
         "AND t.ticketType.event.end < :now")
  int expireTicketsForEndedEvents(
      @Param("newStatus") TicketStatusEnum newStatus,
      @Param("currentStatus") TicketStatusEnum currentStatus,
      @Param("now") LocalDateTime now
  );
}
