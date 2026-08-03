package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {
    private final ShiftService shiftService;

    @GetMapping
    public List<ShiftResponse> findAll() {
        return shiftService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ShiftResponse create(
            @Valid @RequestBody ShiftRequest request,
            Authentication authentication
    ) {
        return shiftService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ShiftResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ShiftRequest request
    ) {
        return shiftService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ShiftResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShiftStatusRequest request,
            Authentication authentication
    ) {
        return shiftService.changeStatus(
                id,
                request,
                authentication.getName()
        );
    }
}
