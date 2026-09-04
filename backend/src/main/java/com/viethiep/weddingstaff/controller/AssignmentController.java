package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.service.AssignmentService;
import com.viethiep.weddingstaff.service.ShiftPlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {
    private final AssignmentService assignmentService;
    private final ShiftPlacementService placementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public List<AssignmentResponse> findAll() {
        return assignmentService.findAll();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<AssignmentResponse> findMine(
            Authentication authentication
    ) {
        return assignmentService.findMine(authentication.getName());
    }

    @PostMapping("/direct")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public AssignmentResponse assignDirectly(
            @Valid @RequestBody DirectAssignmentRequest request,
            Authentication authentication
    ) {
        return assignmentService.assignDirectly(
                request,
                authentication.getName()
        );
    }

    @PatchMapping("/{id}/placement")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public AssignmentPlacementResponse updatePlacement(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentPlacementRequest request
    ) {
        return placementService.updateAssignmentPlacement(id, request);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public AssignmentResponse cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancellationRequest request,
            Authentication authentication
    ) {
        return assignmentService.cancel(
                id,
                request,
                authentication.getName()
        );
    }
}
