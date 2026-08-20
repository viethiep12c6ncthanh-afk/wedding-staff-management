package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotNull;

public record InviteReplacementRequest(
        @NotNull Long employeeId
) {
}
