package com.viethiep.weddingstaff.config;

import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.Role;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import com.viethiep.weddingstaff.enumtype.RoleName;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import com.viethiep.weddingstaff.repository.RoleRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserAccountRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.coordinator-password}")
    private String coordinatorPassword;

    @Value("${app.seed.employee-password}")
    private String employeePassword;

    @Override
    @Transactional
    public void run(String... args) {
        validateSeedPasswords();

        Role adminRole = role(RoleName.ADMIN);
        Role coordinatorRole = role(RoleName.COORDINATOR);
        Role employeeRole = role(RoleName.EMPLOYEE);

        createUser("admin", adminPassword, "Quản trị viên", "admin@example.com", adminRole);
        createUser(
                "coordinator",
                coordinatorPassword,
                "Điều phối viên",
                "coordinator@example.com",
                coordinatorRole
        );
        UserAccount employeeUser = createUser(
                "employee",
                employeePassword,
                "Nhân viên mẫu",
                "employee@example.com",
                employeeRole
        );

        if (!employeeRepository.existsByEmployeeCode("NV001")) {
            employeeRepository.save(Employee.builder()
                    .user(employeeUser)
                    .employeeCode("NV001")
                    .status(EmployeeStatus.ACTIVE)
                    .experienceLevel("Mới")
                    .notes("Tài khoản dữ liệu mẫu")
                    .build());
        }
    }

    private void validateSeedPasswords() {
        if (isBlank(adminPassword)
                || isBlank(coordinatorPassword)
                || isBlank(employeePassword)) {
            throw new IllegalStateException(
                    "SEED_ENABLED=true nhưng một hoặc nhiều mật khẩu seed chưa được cấu hình."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Role role(RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }

    private UserAccount createUser(
            String username,
            String rawPassword,
            String fullName,
            String email,
            Role role
    ) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(
                        UserAccount.builder()
                                .username(username)
                                .password(passwordEncoder.encode(rawPassword))
                                .fullName(fullName)
                                .email(email)
                                .enabled(true)
                                .role(role)
                                .build()
                ));
    }
}
