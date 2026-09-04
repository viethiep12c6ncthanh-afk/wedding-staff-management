package com.viethiep.weddingstaff.dto;

import java.util.List;

public record AssignmentPlacementResponse(
        Long assignmentId,
        Long areaId,
        String areaName,
        List<Long> tableIds,
        List<String> tableCodes,
        String task
) {
}
