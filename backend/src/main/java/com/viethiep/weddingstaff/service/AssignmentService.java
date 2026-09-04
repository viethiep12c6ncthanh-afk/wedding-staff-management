package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.dto.AssignmentResponse;
import com.viethiep.weddingstaff.dto.CancellationRequest;
import com.viethiep.weddingstaff.dto.DirectAssignmentRequest;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.ShiftTable;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AssignmentSource;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.AccountStatus;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import com.viethiep.weddingstaff.enumtype.RoleName;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {
    private static final EnumSet<AssignmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.CONFIRMED);

    private final ShiftAssignmentRepository assignmentRepository;
    private final WorkShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> findAll() {
        return assignmentRepository.findAllWithDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> findMine(String username) {
        return assignmentRepository
                .findAllByEmployeeUsernameWithDetails(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AssignmentResponse assignDirectly(
            DirectAssignmentRequest request,
            String assignerUsername
    ) {
        WorkShift shift = shiftRepository
                .findByIdForUpdate(request.shiftId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy ca")
                );
        validateShiftAssignable(shift);

        Employee employee = employeeRepository
                .findByIdWithUser(request.employeeId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy nhân viên")
                );
        ensureEmployeeAssignable(employee);

        ensureCapacity(shift);
        ensureNoSameShiftAssignment(shift.getId(), employee.getId());
        ensureNoOverlap(employee.getId(), shift);

        UserAccount assigner = userRepository
                .findByUsername(assignerUsername)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy tài khoản phân công")
                );

        ShiftAssignment assignment = ShiftAssignment.builder()
                .shift(shift)
                .employee(employee)
                .registration(null)
                .assignmentSource(AssignmentSource.DIRECT)
                .shiftRole(request.shiftRole())
                .task(request.task())
                .status(AssignmentStatus.ASSIGNED)
                .assignedBy(assigner)
                .build();

        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentResponse cancel(
            Long assignmentId,
            CancellationRequest request,
            String actorUsername
    ) {
        ShiftAssignment assignment = assignmentRepository
                .findByIdForUpdate(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy phân công")
                );

        if (!ACTIVE_STATUSES.contains(assignment.getStatus())) {
            throw new IllegalStateException(
                    "Chỉ được hủy phân công đang hoạt động"
            );
        }
        if (assignment.getShift().getShiftStatus()
                == ShiftStatus.IN_PROGRESS
                || assignment.getShift().getShiftStatus()
                == ShiftStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Không thể hủy phân công khi ca đang diễn ra hoặc đã hoàn thành"
            );
        }

        UserAccount actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy tài khoản")
                );
        LocalDateTime now = LocalDateTime.now();
        String reason = request.reason().trim();

        assignment.setStatus(AssignmentStatus.CANCELLED);
        assignment.setCancelledBy(actor);
        assignment.setCancelledAt(now);
        assignment.setCancellationReason(reason);

        if (assignment.getRegistration() != null
                && assignment.getRegistration().getStatus()
                == com.viethiep.weddingstaff.enumtype.RegistrationStatus.APPROVED) {
            assignment.getRegistration().setStatus(
                    com.viethiep.weddingstaff.enumtype.RegistrationStatus.CANCELLED
            );
            assignment.getRegistration().setCancelledBy(actor);
            assignment.getRegistration().setCancelledAt(now);
            assignment.getRegistration().setCancellationReason(reason);
        }

        return toResponse(assignment);
    }

    private void validateShiftAssignable(WorkShift shift) {
        if (shift.getShiftStatus() != ShiftStatus.OPEN
                && shift.getShiftStatus() != ShiftStatus.CLOSED) {
            throw new IllegalStateException(
                    "Chỉ phân công khi ca đang mở hoặc đã đóng đăng ký"
            );
        }
    }

    private void ensureEmployeeAssignable(Employee employee) {
        if (employee.getEmploymentStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Hồ sơ nhân viên đang không hoạt động"
            );
        }

        UserAccount account = employee.getUser();
        if (account == null || account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Tài khoản nhân viên đang không hoạt động"
            );
        }

        if (account.getRole() == null
                || account.getRole().getName() != RoleName.EMPLOYEE) {
            throw new IllegalStateException(
                    "Tài khoản được phân công phải có vai trò EMPLOYEE"
            );
        }
    }

    private void ensureCapacity(WorkShift shift) {
        long assignedCount = assignmentRepository
                .countByShiftIdAndStatusIn(
                        shift.getId(),
                        ACTIVE_STATUSES
                );
        if (assignedCount >= shift.getRequiredStaff()) {
            throw new IllegalStateException(
                    "Ca đã đủ số lượng nhân viên"
            );
        }
    }

    private void ensureNoSameShiftAssignment(
            Long shiftId,
            Long employeeId
    ) {
        if (assignmentRepository
                .existsByShiftIdAndEmployeeIdAndStatusIn(
                        shiftId,
                        employeeId,
                        ACTIVE_STATUSES
                )) {
            throw new IllegalStateException(
                    "Nhân viên đã được phân công vào ca này"
            );
        }
    }

    private void ensureNoOverlap(Long employeeId, WorkShift shift) {
        if (!assignmentRepository.findOverlaps(
                employeeId,
                shift.getStartAt(),
                shift.getEndAt(),
                ACTIVE_STATUSES
        ).isEmpty()) {
            throw new IllegalStateException(
                    "Nhân viên bị trùng với ca đã được phân công"
            );
        }
    }

    private AssignmentResponse toResponse(ShiftAssignment assignment) {
        List<ShiftTable> tables = assignment.getTables().stream()
                .sorted(Comparator.comparing(
                        ShiftTable::getTableCode,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        return new AssignmentResponse(
                assignment.getId(),
                assignment.getShift().getId(),
                assignment.getShift().getName(),
                assignment.getEmployee().getId(),
                assignment.getEmployee().getUser().getFullName(),
                assignment.getRegistration() == null
                        ? null
                        : assignment.getRegistration().getId(),
                assignment.getAssignmentSource(),
                assignment.getShiftRole(),
                assignment.getShiftArea() == null
                        ? null
                        : assignment.getShiftArea().getName(),
                assignment.getShiftArea() == null
                        ? null
                        : assignment.getShiftArea().getId(),
                tables.stream().map(ShiftTable::getId).toList(),
                tables.stream().map(ShiftTable::getTableCode).toList(),
                assignment.getTask(),
                assignment.getStatus(),
                assignment.getAssignedBy().getUsername(),
                assignment.getCreatedAt(),
                assignment.getCancelledAt(),
                assignment.getCancellationReason()
        );
    }
}
