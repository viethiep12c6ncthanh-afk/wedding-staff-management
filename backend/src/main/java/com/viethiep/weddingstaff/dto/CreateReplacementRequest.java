package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReplacementRequest(
        @NotNull @Positive Long assignmentId,
        @NotBlank @Size(max = 500) String reason
) {
}
