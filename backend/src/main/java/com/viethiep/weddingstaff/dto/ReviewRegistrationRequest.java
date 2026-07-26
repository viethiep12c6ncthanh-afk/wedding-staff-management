package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewRegistrationRequest(
        @NotNull Boolean approved,
        String rejectionReason,
        String roleInShift,
        String area,
        String task
) {}
