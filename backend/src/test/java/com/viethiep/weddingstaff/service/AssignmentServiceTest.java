package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.DirectAssignmentRequest;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.Role;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AccountStatus;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import com.viethiep.weddingstaff.enumtype.RoleName;
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
import static org.mockito.ArgumentMatchers.*;
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
        Employee employee = employee(
                2L,
                AccountStatus.ACTIVE,
                RoleName.EMPLOYEE
        );

        when(shiftRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(shift));
        when(employeeRepository.findByIdWithUser(2L))
                .thenReturn(Optional.of(employee));
        when(assignmentRepository.countByShiftIdAndStatusIn(eq(3L), anyCollection()))
                .thenReturn(1L);

        DirectAssignmentRequest request = new DirectAssignmentRequest(
                3L,
                2L,
                ShiftRole.STAFF,
                "Phục vụ"
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.assignDirectly(request, "admin")
        );

        assertEquals("Ca đã đủ số lượng nhân viên", ex.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void rejectsDirectAssignmentWhenEmployeeAccountIsNotActive() {
        WorkShift shift = shift();
        Employee employee = employee(2L, null, RoleName.EMPLOYEE);

        when(shiftRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(shift));
        when(employeeRepository.findByIdWithUser(2L))
                .thenReturn(Optional.of(employee));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.assignDirectly(
                        new DirectAssignmentRequest(
                                3L,
                                2L,
                                ShiftRole.STAFF,
                                null
                        ),
                        "admin"
                )
        );

        assertEquals(
                "Tài khoản nhân viên đang không hoạt động",
                ex.getMessage()
        );
        verifyNoInteractions(userRepository);
        verify(assignmentRepository, never())
                .countByShiftIdAndStatusIn(anyLong(), anyCollection());
    }

    @Test
    void rejectsDirectAssignmentWhenAccountRoleIsNotEmployee() {
        WorkShift shift = shift();
        Employee employee = employee(
                2L,
                AccountStatus.ACTIVE,
                RoleName.COORDINATOR
        );

        when(shiftRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(shift));
        when(employeeRepository.findByIdWithUser(2L))
                .thenReturn(Optional.of(employee));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.assignDirectly(
                        new DirectAssignmentRequest(
                                3L,
                                2L,
                                ShiftRole.STAFF,
                                null
                        ),
                        "admin"
                )
        );

        assertEquals(
                "Tài khoản được phân công phải có vai trò EMPLOYEE",
                ex.getMessage()
        );
        verifyNoInteractions(userRepository);
    }

    private WorkShift shift() {
        return WorkShift.builder()
                .id(3L)
                .name("Ca tối")
                .startAt(LocalDateTime.of(2026, 8, 10, 18, 0))
                .endAt(LocalDateTime.of(2026, 8, 10, 22, 0))
                .requiredStaff(1)
                .payAmount(new BigDecimal("100000.00"))
                .shiftStatus(ShiftStatus.OPEN)
                .build();
    }

    private Employee employee(
            Long id,
            AccountStatus accountStatus,
            RoleName roleName
    ) {
        UserAccount account = UserAccount.builder()
                .id(id + 100)
                .username("employee" + id)
                .fullName("Nhân viên " + id)
                .accountStatus(accountStatus)
                .role(Role.builder().name(roleName).build())
                .build();

        return Employee.builder()
                .id(id)
                .employeeCode("NV" + id)
                .employmentStatus(EmployeeStatus.ACTIVE)
                .user(account)
                .build();
    }

}
