package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShiftStatusRequest(
        @NotNull ShiftStatus status,
        @Size(max = 500) String cancellationReason
) {
}
