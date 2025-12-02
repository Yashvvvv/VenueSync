package com.fullstack.tickets.repositories;

import com.fullstack.tickets.domain.entities.Ticket;
import com.fullstack.tickets.domain.entities.TicketStatusEnum;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

  int countByTicketTypeId(UUID ticketTypeId);

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
