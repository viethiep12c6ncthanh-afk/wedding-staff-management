package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.CreateEmployeeEvaluationRequest;
import com.viethiep.weddingstaff.dto.EmployeeEvaluationResponse;
import com.viethiep.weddingstaff.dto.ReputationDetailResponse;
import com.viethiep.weddingstaff.dto.ReputationSummaryResponse;
import com.viethiep.weddingstaff.service.ReputationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reputations")
@RequiredArgsConstructor
public class ReputationController {
    private final ReputationService reputationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public List<ReputationSummaryResponse> findAll() {
        return reputationService.findAll();
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ReputationDetailResponse findByEmployeeId(
            @PathVariable Long employeeId
    ) {
        return reputationService.findByEmployeeId(employeeId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ReputationDetailResponse findMine(Authentication authentication) {
        return reputationService.findMine(authentication.getName());
    }

    @PostMapping("/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EmployeeEvaluationResponse createEvaluation(
            @Valid @RequestBody CreateEmployeeEvaluationRequest request,
            Authentication authentication
    ) {
        return reputationService.createEvaluation(
                request,
                authentication.getName()
        );
    }
}
