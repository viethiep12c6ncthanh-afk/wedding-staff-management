package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReplacementRequest(
        @NotNull Long assignmentId,
        @NotBlank @Size(max = 500) String reason
) {
}
