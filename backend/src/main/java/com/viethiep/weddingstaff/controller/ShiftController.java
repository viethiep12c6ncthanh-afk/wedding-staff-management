package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.ShiftRequest;
import com.viethiep.weddingstaff.dto.ShiftResponse;
import com.viethiep.weddingstaff.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {
    private final ShiftService service;

    @GetMapping
    public List<ShiftResponse> findAll() { return service.findAll(); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ShiftResponse create(@Valid @RequestBody ShiftRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ShiftResponse update(@PathVariable Long id, @Valid @RequestBody ShiftRequest request) {
        return service.update(id, request);
    }
}
