package com.fullstack.venuesync.tickets.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fullstack.venuesync.tickets.domain.TicketType;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

  /** Scoped to the event so a ticket type can never be bought through another event's URL. */
  @Query("SELECT tt FROM TicketType tt WHERE tt.id = :id AND tt.event.id = :eventId")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<TicketType> findByIdAndEventIdWithLock(@Param("id") UUID id, @Param("eventId") UUID eventId);
}
