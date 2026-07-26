package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VenueRequest(
        @NotBlank String name,
        @NotBlank String address,
        String contactPhone,
        @NotNull CommonStatus status
) {}
