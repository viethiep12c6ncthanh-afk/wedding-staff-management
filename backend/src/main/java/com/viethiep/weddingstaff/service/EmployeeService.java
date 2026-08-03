package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.EmployeeResponse;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAllWithUser().stream()
                .map(this::toResponse)
                .toList();
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getUser().getFullName(),
                employee.getUser().getEmail(),
                employee.getUser().getPhone(),
                employee.getEmploymentStatus(),
                employee.getDateOfBirth(),
                employee.getAddress(),
                employee.getExperienceLevel(),
                employee.getNote()
        );
    }
}
