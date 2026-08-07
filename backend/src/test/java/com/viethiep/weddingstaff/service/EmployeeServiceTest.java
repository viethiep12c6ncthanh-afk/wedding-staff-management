package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.CreateEmployeeRequest;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import com.viethiep.weddingstaff.repository.RoleRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService service;

    @Test
    void rejectsDuplicateUsernameBeforeCreatingUser() {
        when(userRepository.existsByUsername("nv.commit6")).thenReturn(true);

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "NV.Commit6",
                "Test@1234",
                "Nhân viên test",
                "nv@example.com",
                "0900000001",
                "NV999",
                LocalDate.of(2000, 1, 1),
                "TP.HCM",
                "BEGINNER",
                null
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request, "admin")
        );

        assertEquals("Tên đăng nhập đã tồn tại", ex.getMessage());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(roleRepository, passwordEncoder);
    }
}
