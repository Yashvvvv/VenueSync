package com.fullstack.venuesync.events.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fullstack.venuesync.shared.domain.User;
import com.fullstack.venuesync.tickets.domain.TicketType;

@Entity
// Unique per organizer: a retried create with the same key can only ever make one event. NULL keys (the web, older
// clients) never collide.
@Table(name = "events", uniqueConstraints = @UniqueConstraint(
    name = "uk_events_organizer_idempotency_key", columnNames = {"organizer_id", "idempotency_key"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "event_start")
  private LocalDateTime start;

  @Column(name = "event_end")
  private LocalDateTime end;

  @Column(name = "venue", nullable = false)
  private String venue;

  @Column(name = "sales_start")
  private LocalDateTime salesStart;

  @Column(name = "sales_end")
  private LocalDateTime salesEnd;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private EventStatusEnum status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organizer_id")
  private User organizer;

  /** Client-generated per create attempt; a retry with the same key returns this event instead of a second one. */
  @Column(name = "idempotency_key")
  private UUID idempotencyKey;

  /**
   * Bumped on every change. A client sends back the version it read, and an update made from an older copy is
   * refused (EVENT_CHANGED) instead of silently overwriting someone else's edit. The column default gives rows from
   * before this column a starting version.
   */
  @Version
  @Column(name = "version", nullable = false, columnDefinition = "bigint not null default 0")
  private Long version;

  /** When the photo last changed, or null without one. Written only by EventRepository.setImageUpdatedAt. */
  @Column(name = "image_updated_at")
  private LocalDateTime imageUpdatedAt;

  @ManyToMany(mappedBy = "attendingEvents")
  @Builder.Default
  private List<User> attendees = new ArrayList<>();

  @ManyToMany(mappedBy = "staffingEvents")
  @Builder.Default
  private List<User> staff = new ArrayList<>();

  @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TicketType> ticketTypes = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Where clients load the photo (relative to the API's origin), or null without one. The version in the query means
   * a new photo is a new URL, so the old one can be cached forever.
   */
  public String getImageUrl() {
    return imageUrl(id, imageUpdatedAt);
  }

  public static String imageUrl(UUID id, LocalDateTime imageUpdatedAt) {
    return imageUpdatedAt == null ? null
        : "/api/v1/event-images/" + id + "?v=" + imageUpdatedAt.toInstant(ZoneOffset.UTC).toEpochMilli();
  }

  /**
   * The single source of truth for "can tickets be bought at {@code now}", used by both the
   * purchase check and the public event detail so the two can never disagree.
   */
  public SalesStatus salesStatusAt(LocalDateTime now) {
    if (salesStart != null && now.isBefore(salesStart)) {
      return SalesStatus.UPCOMING;
    }
    if ((salesEnd != null && now.isAfter(salesEnd)) || (end != null && now.isAfter(end))) {
      return SalesStatus.ENDED;
    }
    return SalesStatus.ON_SALE;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Event event = (Event) o;
    return Objects.equals(id, event.id) && Objects.equals(name, event.name) && Objects.equals(start,
        event.start) && Objects.equals(end, event.end) && Objects.equals(venue, event.venue)
        && Objects.equals(salesStart, event.salesStart) && Objects.equals(salesEnd, event.salesEnd)
        && status == event.status && Objects.equals(createdAt, event.createdAt) && Objects.equals(
        updatedAt, event.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, start, end, venue, salesStart, salesEnd, status, createdAt,
        updatedAt);
  }
}
