package com.fullstack.tickets.repositories;

import com.fullstack.tickets.domain.entities.Event;
import com.fullstack.tickets.domain.entities.EventStatusEnum;
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
public interface EventRepository extends JpaRepository<Event, UUID> {

  Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);

  Page<Event> findByOrganizerIdAndStatus(UUID organizerId, EventStatusEnum status, Pageable pageable);

  Optional<Event> findByIdAndOrganizerId(UUID id, UUID organizerId);

  Page<Event> findByStatus(EventStatusEnum status, Pageable pageable);

  @Query(value = "SELECT * FROM events WHERE " +
      "status = 'PUBLISHED' AND " +
      "(LOWER(name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
      "LOWER(venue) LIKE LOWER(CONCAT('%', :searchTerm, '%')))",
      countQuery = "SELECT count(*) FROM events WHERE " +
          "status = 'PUBLISHED' AND " +
          "(LOWER(name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
          "LOWER(venue) LIKE LOWER(CONCAT('%', :searchTerm, '%')))",
      nativeQuery = true)
  Page<Event> searchEvents(@Param("searchTerm") String searchTerm, Pageable pageable);

  Optional<Event> findByIdAndStatus(UUID id, EventStatusEnum status);

  // Count events by status for organizer (for stats)
  long countByOrganizerIdAndStatus(UUID organizerId, EventStatusEnum status);

  /**
   * Automatically marks PUBLISHED events as COMPLETED when their event_end date has passed.
   * Only affects events with status = PUBLISHED and event_end < now.
   * 
   * @param newStatus the status to set (COMPLETED)
   * @param currentStatus the current status to filter (PUBLISHED)
   * @param now the current timestamp
   * @return the number of events updated
   */
  @Modifying
  @Query("UPDATE Event e SET e.status = :newStatus, e.updatedAt = :now " +
         "WHERE e.status = :currentStatus AND e.end < :now AND e.end IS NOT NULL")
  int completeEndedEvents(
      @Param("newStatus") EventStatusEnum newStatus,
      @Param("currentStatus") EventStatusEnum currentStatus,
      @Param("now") LocalDateTime now
  );
}
