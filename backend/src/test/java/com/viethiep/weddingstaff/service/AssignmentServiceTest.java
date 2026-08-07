package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.DirectAssignmentRequest;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import com.viethiep.weddingstaff.enumtype.ShiftRole;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {
    @Mock
    private ShiftAssignmentRepository assignmentRepository;
    @Mock
    private WorkShiftRepository shiftRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserAccountRepository userRepository;

    @InjectMocks
    private AssignmentService service;

    @Test
    void rejectsDirectAssignmentWhenShiftIsAlreadyFull() {
        WorkShift shift = WorkShift.builder()
                .id(3L)
                .name("Ca tối")
                .startAt(LocalDateTime.of(2026, 8, 10, 18, 0))
                .endAt(LocalDateTime.of(2026, 8, 10, 22, 0))
                .requiredStaff(1)
                .payAmount(new BigDecimal("100000.00"))
                .shiftStatus(ShiftStatus.OPEN)
                .build();
        Employee employee = Employee.builder()
                .id(2L)
                .employmentStatus(EmployeeStatus.ACTIVE)
                .build();

        when(shiftRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(shift));
        when(employeeRepository.findById(2L))
                .thenReturn(Optional.of(employee));
        when(assignmentRepository.countByShiftIdAndStatusIn(eq(3L), anyCollection()))
                .thenReturn(1L);

        DirectAssignmentRequest request = new DirectAssignmentRequest(
                3L,
                2L,
                ShiftRole.STAFF,
                "Sảnh chính",
                "Phục vụ"
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.assignDirectly(request, "admin")
        );

        assertEquals("Ca đã đủ số lượng nhân viên", ex.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
    }
}
