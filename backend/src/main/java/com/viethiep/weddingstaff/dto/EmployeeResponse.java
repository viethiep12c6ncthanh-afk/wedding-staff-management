package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.EmployeeStatus;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String fullName,
        String email,
        String phone,
        EmployeeStatus employmentStatus,
        LocalDate dateOfBirth,
        String address,
        String experienceLevel,
        String note
) {
}
