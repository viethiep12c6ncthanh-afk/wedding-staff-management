package com.viethiep.weddingstaff.dto;

import java.util.List;

public record AuditPageResponse(List<AuditLogResponse> content, long totalElements,
                                int totalPages, int page, int size) {
}
