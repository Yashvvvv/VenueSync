package com.fullstack.venuesync.events.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An event's photo, one per event, in its own table so loading an event never loads the bytes.
 *
 * <p>ponytail: the bytes live in Postgres (bytea). No new service or keys, and at demo scale (a few hundred KB per
 * event) it costs nothing. Move them to object storage (Supabase Storage, S3) once there are thousands of events or
 * the database size matters; clients only ever see {@link Event#getImageUrl()}, so that move changes no client.
 */
@Entity
@Table(name = "event_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventImage {

  /** Same as the event's id: the photo has no identity of its own. */
  @Id
  @Column(name = "event_id", nullable = false, updatable = false)
  private UUID eventId;

  @Column(name = "content_type", nullable = false, length = 32)
  private String contentType;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "data", nullable = false, length = EventImages.MAX_BYTES)
  private byte[] data;
}
