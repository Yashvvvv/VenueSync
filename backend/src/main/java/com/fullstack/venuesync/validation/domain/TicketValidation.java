package com.fullstack.venuesync.validation.domain;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
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

import com.fullstack.venuesync.tickets.domain.Ticket;

@Entity
// The key makes a retried scan return the first answer. Postgres allows many NULLs under a unique
// constraint, so scanners that send no key (the web dashboard) are unaffected.
@Table(name = "ticket_validations", uniqueConstraints = @UniqueConstraint(
    name = "uk_ticket_validations_idempotency_key", columnNames = "idempotency_key"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketValidation {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TicketValidationStatusEnum status;

  @Column(name = "validation_method", nullable = false)
  @Enumerated(EnumType.STRING)
  private TicketValidationMethod validationMethod;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id")
  private Ticket ticket;

  @Column(name = "idempotency_key")
  private UUID idempotencyKey;

  @CreatedDate
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

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
    TicketValidation that = (TicketValidation) o;
    return Objects.equals(id, that.id) && status == that.status && Objects.equals(createdAt,
        that.createdAt) && Objects.equals(updatedAt, that.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, createdAt, updatedAt);
  }
}
