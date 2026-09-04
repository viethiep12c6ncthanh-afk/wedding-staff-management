package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateShiftTableRequest(
        @NotBlank @Size(max = 30) String tableCode,
        @Size(max = 100) String displayName,
        @Size(max = 300) String note
) {
}
