package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.CancellationRequest;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.ShiftRegistration;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.RegistrationStatus;
import com.viethiep.weddingstaff.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock
    private ShiftRegistrationRepository registrationRepository;
    @Mock
    private ShiftAssignmentRepository assignmentRepository;
    @Mock
    private WorkShiftRepository shiftRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserAccountRepository userRepository;

    @InjectMocks
    private RegistrationService service;

    @Test
    void rejectsEmployeeCancellationInsideTwentyFourHours() {
        UserAccount user = UserAccount.builder()
                .username("employee")
                .fullName("Nhân viên mẫu")
                .build();
        Employee employee = Employee.builder()
                .id(1L)
                .user(user)
                .build();
        WorkShift shift = WorkShift.builder()
                .id(1L)
                .startAt(LocalDateTime.now().plusHours(12))
                .build();
        ShiftRegistration registration = ShiftRegistration.builder()
                .id(1L)
                .employee(employee)
                .shift(shift)
                .status(RegistrationStatus.APPROVED)
                .build();

        when(registrationRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(registration));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.cancelMine(
                        1L,
                        new CancellationRequest("Có việc đột xuất"),
                        "employee"
                )
        );

        assertEquals(
                "Chỉ được hủy đăng ký trước giờ bắt đầu ca ít nhất 24 giờ",
                ex.getMessage()
        );
        verifyNoInteractions(userRepository);
    }
}
