package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateShiftTableRequest(
        @NotNull @Positive Long areaId,
        @NotBlank @Size(max = 30) String tableCode,
        @Size(max = 100) String displayName,
        @Size(max = 300) String note
) {
}
