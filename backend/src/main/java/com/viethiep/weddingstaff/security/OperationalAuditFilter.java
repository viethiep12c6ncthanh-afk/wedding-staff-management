package com.viethiep.weddingstaff.security;

import com.viethiep.weddingstaff.service.OperationalAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OperationalAuditFilter extends OncePerRequestFilter {
    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final OperationalAuditService auditService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            if (MUTATIONS.contains(request.getMethod()) && request.getRequestURI().startsWith("/api/")) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
                String role = auth == null ? null : auth.getAuthorities().stream()
                        .findFirst().map(Object::toString).orElse(null);
                try {
                    auditService.record(username, role, request.getMethod(), request.getRequestURI(),
                            response.getStatus(), clientIp(request));
                } catch (RuntimeException ignored) {
                    // Audit failure must not roll back the business request.
                }
            }
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
