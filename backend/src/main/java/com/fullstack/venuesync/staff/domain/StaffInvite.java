package com.fullstack.venuesync.staff.domain;

import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.shared.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A one-time code an organizer hands to one person to join their event's door staff. Single-use and expiring:
 * a code that leaks stops working once redeemed, and at the latest after {@code expiresAt}.
 */
@Entity
@Table(name = "staff_invites", uniqueConstraints = @UniqueConstraint(name = "uk_staff_invites_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
public class StaffInvite {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  /** Normalized form: 10 Crockford base32 characters, no separator (see StaffInviteCodes). */
  @Column(name = "code", nullable = false, length = 10)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "accepted_by")
  private User acceptedBy;

  @Column(name = "accepted_at")
  private LocalDateTime acceptedAt;
}
