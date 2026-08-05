package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String temporaryPassword,
        @NotBlank @Size(max = 120) String fullName,
        @Email @Size(max = 120) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(max = 30) String employeeCode,
        @Past LocalDate dateOfBirth,
        @Size(max = 255) String address,
        @Size(max = 50) String experienceLevel,
        @Size(max = 500) String note
) {
}
