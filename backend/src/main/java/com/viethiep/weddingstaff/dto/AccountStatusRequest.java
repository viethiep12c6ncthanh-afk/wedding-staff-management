package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(
        @NotNull AccountStatus status
) {
}
