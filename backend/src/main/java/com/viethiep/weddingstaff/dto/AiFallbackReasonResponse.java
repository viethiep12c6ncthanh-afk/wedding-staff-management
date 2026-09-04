package com.viethiep.weddingstaff.dto;

public record AiFallbackReasonResponse(
        String reason,
        long runCount
) {
}
