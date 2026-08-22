package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.service.CandidateRecommendationService;
import com.viethiep.weddingstaff.service.ReplacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replacements")
@RequiredArgsConstructor
public class ReplacementController {
    private final ReplacementService replacementService;
    private final CandidateRecommendationService candidateRecommendationService;

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public List<ReplacementRequestResponse> findAllRequests() {
        return replacementService.findAllRequests();
    }


    @GetMapping("/requests/{id}/candidates")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public List<ReplacementCandidateResponse> findCandidates(
            @PathVariable Long id
    ) {
        return candidateRecommendationService.findCandidates(id);
    }

    @GetMapping("/requests/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<ReplacementRequestResponse> findMyRequests(
            Authentication authentication
    ) {
        return replacementService.findMyRequests(authentication.getName());
    }

    @GetMapping("/eligible-assignments/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<ReplacementEligibleAssignmentResponse> findMyEligibleAssignments(
            Authentication authentication
    ) {
        return replacementService.findMyEligibleAssignments(authentication.getName());
    }

    @PostMapping("/requests")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ReplacementRequestResponse createRequest(
            @Valid @RequestBody CreateReplacementRequest request,
            Authentication authentication
    ) {
        return replacementService.createRequest(request, authentication.getName());
    }

    @PutMapping("/requests/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ReplacementRequestResponse reviewRequest(
            @PathVariable Long id,
            @Valid @RequestBody ReviewReplacementRequest request,
            Authentication authentication
    ) {
        return replacementService.reviewRequest(id, request, authentication.getName());
    }

    @PostMapping("/requests/{id}/invitations")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public ReplacementRequestResponse inviteEmployee(
            @PathVariable Long id,
            @Valid @RequestBody InviteReplacementRequest request,
            Authentication authentication
    ) {
        return replacementService.inviteEmployee(id, request, authentication.getName());
    }

    @GetMapping("/invitations/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<ReplacementInvitationResponse> findMyInvitations(
            Authentication authentication
    ) {
        return replacementService.findMyInvitations(authentication.getName());
    }

    @PutMapping("/invitations/{id}/respond")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ReplacementInvitationResponse respondInvitation(
            @PathVariable Long id,
            @Valid @RequestBody RespondReplacementInvitationRequest request,
            Authentication authentication
    ) {
        return replacementService.respondInvitation(id, request, authentication.getName());
    }
}
