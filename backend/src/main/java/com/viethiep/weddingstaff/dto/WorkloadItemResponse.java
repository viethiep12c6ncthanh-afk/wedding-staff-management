package com.viethiep.weddingstaff.dto;

public record WorkloadItemResponse(
        Long employeeId,
        String employeeCode,
        String fullName,
        long confirmedShiftCount,
        long paidShiftCount
) {
}
