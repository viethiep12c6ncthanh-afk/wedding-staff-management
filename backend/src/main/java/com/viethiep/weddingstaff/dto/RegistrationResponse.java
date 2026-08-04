package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.RegistrationStatus;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long shiftId,
        String shiftName,
        Long employeeId,
        String employeeName,
        RegistrationStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String rejectionReason,
        LocalDateTime cancelledAt,
        String cancellationReason
) {
}
