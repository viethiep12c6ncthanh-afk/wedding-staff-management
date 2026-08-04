package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.AssignmentResponse;
import com.viethiep.weddingstaff.dto.CancellationRequest;
import com.viethiep.weddingstaff.dto.DirectAssignmentRequest;
import com.viethiep.weddingstaff.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
public class AssignmentController {
    private final AssignmentService assignmentService;

    @GetMapping
    public List<AssignmentResponse> findAll() {
        return assignmentService.findAll();
    }

    @PostMapping("/direct")
    public AssignmentResponse assignDirectly(
            @Valid @RequestBody DirectAssignmentRequest request,
            Authentication authentication
    ) {
        return assignmentService.assignDirectly(
                request,
                authentication.getName()
        );
    }

    @PatchMapping("/{id}/cancel")
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
