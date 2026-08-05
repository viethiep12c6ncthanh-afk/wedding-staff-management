package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AccountStatus;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        Long userId,
        String employeeCode,
        String username,
        String fullName,
        String email,
        String phone,
        AccountStatus accountStatus,
        boolean mustChangePassword,
        EmployeeStatus employmentStatus,
        LocalDate dateOfBirth,
        String address,
        String experienceLevel,
        String note
) {
}
