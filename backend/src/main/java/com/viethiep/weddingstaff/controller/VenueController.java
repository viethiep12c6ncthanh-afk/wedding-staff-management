package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {
    private final VenueService venueService;

    @GetMapping
    public List<VenueResponse> findAll() {
        return venueService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public VenueResponse create(
            @Valid @RequestBody VenueRequest request,
            Authentication authentication
    ) {
        return venueService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public VenueResponse update(
            @PathVariable Long id,
            @Valid @RequestBody VenueRequest request
    ) {
        return venueService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public VenueResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody VenueStatusRequest request
    ) {
        return venueService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable Long id) {
        venueService.deactivate(id);
    }
}
