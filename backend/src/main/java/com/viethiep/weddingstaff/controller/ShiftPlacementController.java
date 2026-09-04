package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.service.ShiftPlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/placements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
public class ShiftPlacementController {
    private final ShiftPlacementService placementService;

    @GetMapping("/areas")
    public List<ShiftAreaResponse> findAreas(
            @RequestParam Long shiftId
    ) {
        return placementService.findAreas(shiftId);
    }

    @PostMapping("/areas")
    public ShiftAreaResponse createArea(
            @Valid @RequestBody CreateShiftAreaRequest request,
            Authentication authentication
    ) {
        return placementService.createArea(
                request,
                authentication.getName()
        );
    }

    @PutMapping("/areas/{id}")
    public ShiftAreaResponse updateArea(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShiftAreaRequest request
    ) {
        return placementService.updateArea(id, request);
    }

    @PatchMapping("/areas/{id}/status")
    public ShiftAreaResponse changeAreaStatus(
            @PathVariable Long id,
            @Valid @RequestBody PlacementStatusRequest request
    ) {
        return placementService.changeAreaStatus(id, request);
    }

    @PostMapping("/tables")
    public ShiftTableResponse createTable(
            @Valid @RequestBody CreateShiftTableRequest request,
            Authentication authentication
    ) {
        return placementService.createTable(
                request,
                authentication.getName()
        );
    }

    @PutMapping("/tables/{id}")
    public ShiftTableResponse updateTable(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShiftTableRequest request
    ) {
        return placementService.updateTable(id, request);
    }

    @PatchMapping("/tables/{id}/status")
    public ShiftTableResponse changeTableStatus(
            @PathVariable Long id,
            @Valid @RequestBody PlacementStatusRequest request
    ) {
        return placementService.changeTableStatus(id, request);
    }
}
