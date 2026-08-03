package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public List<EventResponse> findAll() {
        return eventService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EventResponse create(
            @Valid @RequestBody EventRequest request,
            Authentication authentication
    ) {
        return eventService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EventResponse update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request
    ) {
        return eventService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EventResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody EventStatusRequest request,
            Authentication authentication
    ) {
        return eventService.changeStatus(
                id,
                request,
                authentication.getName()
        );
    }
}
