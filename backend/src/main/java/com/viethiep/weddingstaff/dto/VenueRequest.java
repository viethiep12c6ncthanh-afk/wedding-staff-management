package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VenueRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 300) String address,
        @Size(max = 120) String contactName,
        @Size(max = 20) String contactPhone,
        @Size(max = 500) String note
) {
}
