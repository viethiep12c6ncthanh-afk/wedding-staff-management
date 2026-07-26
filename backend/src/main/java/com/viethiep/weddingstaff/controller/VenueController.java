package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.VenueRequest;
import com.viethiep.weddingstaff.dto.VenueResponse;
import com.viethiep.weddingstaff.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {
    private final VenueService service;

    @GetMapping
    public List<VenueResponse> findAll() { return service.findAll(); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public VenueResponse create(@Valid @RequestBody VenueRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public VenueResponse update(@PathVariable Long id, @Valid @RequestBody VenueRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
