package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.EventRequest;
import com.viethiep.weddingstaff.dto.EventResponse;
import com.viethiep.weddingstaff.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService service;

    @GetMapping
    public List<EventResponse> findAll() { return service.findAll(); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EventResponse create(@Valid @RequestBody EventRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return service.update(id, request);
    }
}
