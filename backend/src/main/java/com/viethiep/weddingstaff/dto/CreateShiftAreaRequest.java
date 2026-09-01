package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateShiftAreaRequest(
        @NotNull @Positive Long shiftId,
        @NotBlank @Size(max = 100) String name,
        @Min(1) Integer requiredStaff,
        @Size(max = 500) String description
) {
}
