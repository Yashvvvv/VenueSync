package com.fullstack.venuesync.staff.repository;

import com.fullstack.venuesync.staff.domain.StaffInvite;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffInviteRepository extends JpaRepository<StaffInvite, UUID> {

  boolean existsByCode(String code);

  /** An event's invites go with it (see EventServiceImpl.deleteEventForOrganizer). */
  @Modifying
  @Query("DELETE FROM StaffInvite i WHERE i.event.id = :eventId")
  int deleteByEventId(@Param("eventId") UUID eventId);

  /** Row lock: two people redeeming one code at the same moment are serialized, so only one can win it. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM StaffInvite i WHERE i.code = :code")
  Optional<StaffInvite> findByCodeForUpdate(@Param("code") String code);
}
