package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeRepository repository;

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public List<Map<String, Object>> findAll() {
        return repository.findAll().stream().map(this::view).toList();
    }

    private Map<String, Object> view(Employee employee) {
        return Map.of(
                "id", employee.getId(),
                "employeeCode", employee.getEmployeeCode(),
                "fullName", employee.getUser().getFullName(),
                "email", employee.getUser().getEmail() == null ? "" : employee.getUser().getEmail(),
                "status", employee.getStatus(),
                "experienceLevel", employee.getExperienceLevel() == null ? "" : employee.getExperienceLevel());
    }
}
