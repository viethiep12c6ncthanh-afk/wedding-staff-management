package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.CancellationRequest;
import com.viethiep.weddingstaff.dto.RegistrationRequest;
import com.viethiep.weddingstaff.dto.RegistrationResponse;
import com.viethiep.weddingstaff.dto.ReviewRegistrationRequest;
import com.viethiep.weddingstaff.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public List<RegistrationResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<RegistrationResponse> findMine(
            Authentication authentication
    ) {
        return service.findMine(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public RegistrationResponse register(
            @Valid @RequestBody RegistrationRequest request,
            Authentication authentication
    ) {
        return service.register(
                request.shiftId(),
                authentication.getName()
        );
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public RegistrationResponse review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRegistrationRequest request,
            Authentication authentication
    ) {
        return service.review(
                id,
                request,
                authentication.getName()
        );
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public RegistrationResponse cancelMine(
            @PathVariable Long id,
            @Valid @RequestBody CancellationRequest request,
            Authentication authentication
    ) {
        return service.cancelMine(
                id,
                request,
                authentication.getName()
        );
    }
}
