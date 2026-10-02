package com.fullstack.venuesync.shared.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /** Whether the user is on the event's door staff (user_staffing_events). */
  @Query("SELECT COUNT(e) > 0 FROM User u JOIN u.staffingEvents e WHERE u.id = :userId AND e.id = :eventId")
  boolean isStaffOf(@Param("userId") UUID userId, @Param("eventId") UUID eventId);
}
