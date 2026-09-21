package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.AuditPageResponse;
import com.viethiep.weddingstaff.service.OperationalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditController {
    private final OperationalAuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AuditPageResponse findAll(@RequestParam(required = false) String actor,
                                     @RequestParam(required = false) String action,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "25") int size) {
        return auditService.findAll(actor, action, page, size);
    }
}
