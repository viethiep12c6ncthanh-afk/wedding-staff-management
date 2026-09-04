package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.CommonStatus;
import jakarta.validation.constraints.NotNull;

public record PlacementStatusRequest(
        @NotNull CommonStatus status
) {
}
