package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.AccountStatusRequest;
import com.viethiep.weddingstaff.dto.CreateEmployeeRequest;
import com.viethiep.weddingstaff.dto.EmployeeResponse;
import com.viethiep.weddingstaff.dto.EmployeeStatusRequest;
import com.viethiep.weddingstaff.dto.UpdateEmployeeRequest;
import com.viethiep.weddingstaff.enumtype.AccountStatus;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import com.viethiep.weddingstaff.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public List<EmployeeResponse> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EmployeeStatus employmentStatus,
            @RequestParam(required = false) AccountStatus accountStatus
    ) {
        return employeeService.findAll(keyword, employmentStatus, accountStatus);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EmployeeResponse findById(@PathVariable Long id) {
        return employeeService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EmployeeResponse create(
            @Valid @RequestBody CreateEmployeeRequest request,
            Authentication authentication
    ) {
        return employeeService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EmployeeResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return employeeService.update(id, request);
    }

    @PatchMapping("/{id}/employment-status")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public EmployeeResponse changeEmploymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeStatusRequest request
    ) {
        return employeeService.changeEmploymentStatus(id, request);
    }

    @PatchMapping("/{id}/account-status")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse changeAccountStatus(
            @PathVariable Long id,
            @Valid @RequestBody AccountStatusRequest request
    ) {
        return employeeService.changeAccountStatus(id, request);
    }
}
