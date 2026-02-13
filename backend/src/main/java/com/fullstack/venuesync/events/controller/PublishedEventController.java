package com.fullstack.venuesync.events.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fullstack.venuesync.events.dto.GetPublishedEventDetailsResponseDto;
import com.fullstack.venuesync.events.dto.ListPublishedEventResponseDto;
import com.fullstack.venuesync.events.domain.Event;
import com.fullstack.venuesync.events.mapper.EventMapper;
import com.fullstack.venuesync.events.service.EventService;

@RestController
@RequestMapping(path = "/api/v1/published-events")
@RequiredArgsConstructor
public class PublishedEventController {

  private final EventService eventService;
  private final EventMapper eventMapper;

  @GetMapping
  public ResponseEntity<Page<ListPublishedEventResponseDto>> listPublishedEvents(
      @RequestParam(required = false) String q,
      Pageable pageable) {

    Page<Event> events;
    if (null != q && !q.trim().isEmpty()) {
      events = eventService.searchPublishedEvents(q, pageable);
    } else {
      events = eventService.listPublishedEvents(pageable);
    }

    return ResponseEntity.ok(
        events.map(eventMapper::toListPublishedEventResponseDto)
    );
  }

  @GetMapping(path = "/{eventId}")
  public ResponseEntity<GetPublishedEventDetailsResponseDto> getPublishedEventDetails(
      @PathVariable UUID eventId
  ) {
    return eventService.getPublishedEvent(eventId)
        .map(eventMapper::toGetPublishedEventDetailsResponseDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
