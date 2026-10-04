package com.fullstack.venuesync.events.repository;

import com.fullstack.venuesync.events.domain.EventImage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventImageRepository extends JpaRepository<EventImage, UUID> {}
