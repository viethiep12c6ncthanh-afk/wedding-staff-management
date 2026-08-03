package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.EventStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventStatusRequest(
        @NotNull EventStatus status,
        @Size(max = 500) String cancellationReason
) {
}
