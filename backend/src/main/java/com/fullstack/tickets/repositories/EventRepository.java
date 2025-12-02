package com.fullstack.tickets.repositories;

import com.fullstack.tickets.domain.entities.Event;
import com.fullstack.tickets.domain.entities.EventStatusEnum;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

  Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);

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
}
