package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InviteReplacementRequest(
        @NotNull @Positive Long employeeId
) {
}
