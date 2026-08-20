package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RespondReplacementInvitationRequest(
        @NotNull Boolean accepted,
        @Size(max = 500) String responseNote
) {
}
