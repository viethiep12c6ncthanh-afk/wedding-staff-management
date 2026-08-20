package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewReplacementRequest(
        @NotNull Boolean approved,
        @Size(max = 500) String reviewNote
) {
}
