package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record EmployeeStatusRequest(
        @NotNull EmployeeStatus status
) {
}
