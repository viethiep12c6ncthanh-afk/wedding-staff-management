package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ShiftRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRegistrationRequest(
        @NotNull Boolean approved,
        @Size(max = 500) String rejectionReason,
        ShiftRole shiftRole,
        @Size(max = 300) String task
) {
}
