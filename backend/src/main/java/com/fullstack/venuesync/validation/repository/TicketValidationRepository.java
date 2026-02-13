package com.fullstack.venuesync.validation.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fullstack.venuesync.validation.domain.TicketValidation;

@Repository
public interface  TicketValidationRepository extends JpaRepository<TicketValidation, UUID> {
}
