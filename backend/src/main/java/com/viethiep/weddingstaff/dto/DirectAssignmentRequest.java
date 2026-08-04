package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ShiftRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DirectAssignmentRequest(
        @NotNull Long shiftId,
        @NotNull Long employeeId,
        @NotNull ShiftRole shiftRole,
        @Size(max = 100) String area,
        @Size(max = 300) String task
) {
}
