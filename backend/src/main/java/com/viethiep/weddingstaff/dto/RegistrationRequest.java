package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(@NotNull Long shiftId) {}
