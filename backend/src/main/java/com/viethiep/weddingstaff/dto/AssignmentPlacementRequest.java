package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssignmentPlacementRequest(
        @Positive Long areaId,
        @Size(max = 50) List<@Positive Long> tableIds,
        @Size(max = 300) String task
) {
}
