package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.EventRequest;
import com.viethiep.weddingstaff.dto.EventResponse;
import com.viethiep.weddingstaff.entity.Event;
import com.viethiep.weddingstaff.entity.Venue;
import com.viethiep.weddingstaff.repository.EventRepository;
import com.viethiep.weddingstaff.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        validateTimes(request);
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa điểm"));
        Event event = Event.builder()
                .venue(venue).name(request.name()).startAt(request.startAt()).endAt(request.endAt())
                .status(request.status()).description(request.description()).build();
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        validateTimes(request);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện"));
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa điểm"));
        event.setVenue(venue);
        event.setName(request.name());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setStatus(request.status());
        event.setDescription(request.description());
        return toResponse(event);
    }

    private void validateTimes(EventRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(e.getId(), e.getVenue().getId(), e.getVenue().getName(), e.getName(),
                e.getStartAt(), e.getEndAt(), e.getStatus(), e.getDescription());
    }
}
