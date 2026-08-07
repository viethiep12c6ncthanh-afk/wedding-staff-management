package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.dto.AccountStatusRequest;
import com.viethiep.weddingstaff.dto.CreateEmployeeRequest;
import com.viethiep.weddingstaff.dto.EmployeeResponse;
import com.viethiep.weddingstaff.dto.EmployeeStatusRequest;
import com.viethiep.weddingstaff.dto.UpdateEmployeeRequest;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.Role;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.enumtype.AccountStatus;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import com.viethiep.weddingstaff.enumtype.RoleName;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import com.viethiep.weddingstaff.repository.RoleRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll(
            String keyword,
            EmployeeStatus employmentStatus,
            AccountStatus accountStatus
    ) {
        return employeeRepository.search(
                        normalizeKeyword(keyword),
                        employmentStatus,
                        accountStatus
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return toResponse(findEmployee(id));
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request, String creatorUsername) {
        String username = normalizeUsername(request.username());
        String employeeCode = request.employeeCode().trim().toUpperCase(Locale.ROOT);
        String email = normalizeOptional(request.email());
        String phone = normalizeOptional(request.phone());

        validateCreateUniqueness(username, employeeCode, email, phone);

        UserAccount creator = findUser(creatorUsername);
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình role EMPLOYEE"));

        UserAccount user = userRepository.save(UserAccount.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.temporaryPassword()))
                .fullName(request.fullName().trim())
                .email(email)
                .phone(phone)
                .accountStatus(AccountStatus.ACTIVE)
                .mustChangePassword(true)
                .createdBy(creator)
                .role(employeeRole)
                .build());

        Employee employee = Employee.builder()
                .user(user)
                .employeeCode(employeeCode)
                .employmentStatus(EmployeeStatus.ACTIVE)
                .dateOfBirth(request.dateOfBirth())
                .address(normalizeOptional(request.address()))
                .experienceLevel(normalizeOptional(request.experienceLevel()))
                .note(normalizeOptional(request.note()))
                .build();

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        Employee employee = findEmployee(id);
        UserAccount user = employee.getUser();

        String email = normalizeOptional(request.email());
        String phone = normalizeOptional(request.phone());
        validateUpdateUniqueness(user.getId(), email, phone);

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(phone);

        employee.setDateOfBirth(request.dateOfBirth());
        employee.setAddress(normalizeOptional(request.address()));
        employee.setExperienceLevel(normalizeOptional(request.experienceLevel()));
        employee.setNote(normalizeOptional(request.note()));

        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse changeEmploymentStatus(Long id, EmployeeStatusRequest request) {
        Employee employee = findEmployee(id);
        employee.setEmploymentStatus(request.status());
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse changeAccountStatus(Long id, AccountStatusRequest request) {
        Employee employee = findEmployee(id);
        employee.getUser().setAccountStatus(request.status());
        return toResponse(employee);
    }

    private void validateCreateUniqueness(
            String username,
            String employeeCode,
            String email,
            String phone
    ) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new IllegalArgumentException("Mã nhân viên đã tồn tại");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }
    }

    private void validateUpdateUniqueness(Long userId, String email, String phone) {
        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên"));
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    private String normalizeUsername(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeKeyword(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private EmployeeResponse toResponse(Employee employee) {
        UserAccount user = employee.getUser();
        return new EmployeeResponse(
                employee.getId(),
                user.getId(),
                employee.getEmployeeCode(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAccountStatus(),
                user.isMustChangePassword(),
                employee.getEmploymentStatus(),
                employee.getDateOfBirth(),
                employee.getAddress(),
                employee.getExperienceLevel(),
                employee.getNote()
        );
    }
}
