package com.viethiep.weddingstaff.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, String actorUsername, String actorRole,
                               String action, String resourcePath, Integer httpStatus,
                               boolean success, String clientIp, LocalDateTime occurredAt) {
}
