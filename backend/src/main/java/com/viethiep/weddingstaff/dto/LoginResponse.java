package com.viethiep.weddingstaff.dto;

public record LoginResponse(
        String token,
        String username,
        String fullName,
        String role
) {}
