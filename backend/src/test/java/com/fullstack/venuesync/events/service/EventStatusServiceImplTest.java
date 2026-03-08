package com.fullstack.venuesync.events.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fullstack.venuesync.events.domain.EventStatusEnum;
import com.fullstack.venuesync.events.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
class EventStatusServiceImplTest {

  @Mock
  private EventRepository eventRepository;

  @InjectMocks
  private EventStatusServiceImpl eventStatusService;

  @Test
  @DisplayName("should complete ended events and return count")
  void shouldCompleteEndedEvents() {
    when(eventRepository.completeEndedEvents(
        eq(EventStatusEnum.COMPLETED),
        eq(EventStatusEnum.PUBLISHED),
        any(LocalDateTime.class)
    )).thenReturn(3);

    int result = eventStatusService.completeEndedEvents();

    assertEquals(3, result);
    verify(eventRepository).completeEndedEvents(
        eq(EventStatusEnum.COMPLETED),
        eq(EventStatusEnum.PUBLISHED),
        any(LocalDateTime.class)
    );
  }

  @Test
  @DisplayName("should return zero when no events to complete")
  void shouldReturnZeroWhenNoEventsToComplete() {
    when(eventRepository.completeEndedEvents(
        eq(EventStatusEnum.COMPLETED),
        eq(EventStatusEnum.PUBLISHED),
        any(LocalDateTime.class)
    )).thenReturn(0);

    int result = eventStatusService.completeEndedEvents();

    assertEquals(0, result);
  }
}
