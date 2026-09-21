package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.AuditLogResponse;
import com.viethiep.weddingstaff.service.OperationalAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final OperationalAuditService auditService;

    @GetMapping("/recent")
    public List<AuditLogResponse> recent(Authentication authentication) {
        boolean manager = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_COORDINATOR"));
        return auditService.recent(authentication.getName(), manager);
    }
}
