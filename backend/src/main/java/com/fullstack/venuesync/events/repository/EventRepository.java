package com.fullstack.venuesync.events.repository;

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

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.domain.EventStatusEnum;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

  Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);

  Page<Event> findByOrganizerIdAndStatus(UUID organizerId, EventStatusEnum status, Pageable pageable);

  Optional<Event> findByIdAndOrganizerId(UUID id, UUID organizerId);

  Optional<Event> findByOrganizerIdAndIdempotencyKey(UUID organizerId, UUID idempotencyKey);

  boolean existsByIdAndOrganizerId(UUID id, UUID organizerId);

  /** Events whose door this user may work: the ones they organize plus the ones they staff. */
  @Query("SELECT DISTINCT e FROM Event e LEFT JOIN e.staff s "
      + "WHERE e.status = :status AND (e.organizer.id = :userId OR s.id = :userId) ORDER BY e.start ASC")
  List<Event> findScannableBy(@Param("userId") UUID userId, @Param("status") EventStatusEnum status);

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

  /** The demo catalogue finds its own events by name and venue together, which no organizer's event shares. */
  Optional<Event> findFirstByNameAndVenue(String name, String venue);

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
  /**
   * Records that the photo changed. A bulk update on purpose: it leaves the event's version alone (a new photo isn't
   * an edit, and bumping it would make an organizer's open form refuse to save).
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE Event e SET e.imageUpdatedAt = :at WHERE e.id = :id")
  int setImageUpdatedAt(@Param("id") UUID id, @Param("at") LocalDateTime at);

  @Modifying
  @Query("UPDATE Event e SET e.status = :newStatus, e.updatedAt = :now " +
         "WHERE e.status = :currentStatus AND e.end < :now AND e.end IS NOT NULL")
  int completeEndedEvents(
      @Param("newStatus") EventStatusEnum newStatus,
      @Param("currentStatus") EventStatusEnum currentStatus,
      @Param("now") LocalDateTime now
  );
}
