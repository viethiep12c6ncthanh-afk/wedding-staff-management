package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.CommonStatus;

import java.util.List;

public record ShiftAreaResponse(
        Long id,
        Long shiftId,
        String shiftName,
        String name,
        Integer requiredStaff,
        String description,
        CommonStatus status,
        long assignedStaffCount,
        List<ShiftTableResponse> tables
) {
}
