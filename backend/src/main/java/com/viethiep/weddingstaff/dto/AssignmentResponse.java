package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AssignmentSource;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.ShiftRole;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentResponse(
        Long id,
        Long shiftId,
        String shiftName,
        Long employeeId,
        String employeeName,
        Long registrationId,
        AssignmentSource assignmentSource,
        ShiftRole shiftRole,
        String area,
        Long areaId,
        List<Long> tableIds,
        List<String> tableCodes,
        String task,
        AssignmentStatus status,
        String assignedBy,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        String cancellationReason
) {
}
