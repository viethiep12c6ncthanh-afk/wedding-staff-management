package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.CancellationRequest;
import com.viethiep.weddingstaff.dto.RegistrationResponse;
import com.viethiep.weddingstaff.dto.ReviewRegistrationRequest;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.ShiftRegistration;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AssignmentSource;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import com.viethiep.weddingstaff.enumtype.RegistrationStatus;
import com.viethiep.weddingstaff.enumtype.ShiftRole;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.ShiftRegistrationRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private static final EnumSet<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.CONFIRMED);

    private final ShiftRegistrationRepository registrationRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final WorkShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public List<RegistrationResponse> findAll() {
        return registrationRepository.findAllWithDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> findMine(String username) {
        return registrationRepository.findAllByEmployeeUsername(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RegistrationResponse register(Long shiftId, String username) {
        Employee employee = findEmployeeByUsername(username);
        ensureEmployeeActive(employee);

        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy ca")
                );

        if (shift.getShiftStatus() != ShiftStatus.OPEN) {
            throw new IllegalStateException("Ca chưa mở đăng ký");
        }
        if (shift.getRegistrationDeadline() != null
                && !LocalDateTime.now().isBefore(
                        shift.getRegistrationDeadline()
                )) {
            throw new IllegalStateException("Đã quá hạn đăng ký");
        }
        if (registrationRepository.existsByShiftIdAndEmployeeId(
                shiftId,
                employee.getId()
        )) {
            throw new IllegalStateException(
                    "Nhân viên đã có lịch sử đăng ký cho ca này"
            );
        }
        ensureNoAssignmentOverlap(employee.getId(), shift);

        ShiftRegistration registration = ShiftRegistration.builder()
                .shift(shift)
                .employee(employee)
                .status(RegistrationStatus.PENDING)
                .build();

        return toResponse(registrationRepository.save(registration));
    }

    @Transactional
    public RegistrationResponse review(
            Long registrationId,
            ReviewRegistrationRequest request,
            String reviewerUsername
    ) {
        ShiftRegistration registration = registrationRepository
                .findByIdForUpdate(registrationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy đăng ký"
                        )
                );

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException("Đăng ký đã được xử lý");
        }

        WorkShift shift = shiftRepository
                .findByIdForUpdate(registration.getShift().getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy ca")
                );
        validateShiftReviewable(shift);

        UserAccount reviewer = findUser(reviewerUsername);
        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(request.approved())) {
            ShiftRole shiftRole = request.shiftRole() == null
                    ? ShiftRole.STAFF
                    : request.shiftRole();

            ensureCapacity(shift);
            ensureNoActiveAssignmentForSameShift(
                    shift.getId(),
                    registration.getEmployee().getId()
            );
            ensureNoAssignmentOverlap(
                    registration.getEmployee().getId(),
                    shift
            );

            registration.setStatus(RegistrationStatus.APPROVED);
            registration.setRejectionReason(null);

            ShiftAssignment assignment = ShiftAssignment.builder()
                    .shift(shift)
                    .employee(registration.getEmployee())
                    .registration(registration)
                    .assignmentSource(AssignmentSource.REGISTRATION)
                    .shiftRole(shiftRole)
                    .area(request.area())
                    .task(request.task())
                    .status(AssignmentStatus.ASSIGNED)
                    .assignedBy(reviewer)
                    .build();
            assignmentRepository.save(assignment);
        } else {
            requireReason(
                    request.rejectionReason(),
                    "Bắt buộc nhập lý do từ chối đăng ký"
            );
            registration.setStatus(RegistrationStatus.REJECTED);
            registration.setRejectionReason(
                    request.rejectionReason().trim()
            );
        }

        registration.setReviewedBy(reviewer);
        registration.setReviewedAt(now);
        return toResponse(registration);
    }

    @Transactional
    public RegistrationResponse cancelMine(
            Long registrationId,
            CancellationRequest request,
            String username
    ) {
        ShiftRegistration registration = registrationRepository
                .findByIdForUpdate(registrationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy đăng ký"
                        )
                );

        if (!registration.getEmployee().getUser().getUsername()
                .equals(username)) {
            throw new IllegalStateException(
                    "Không thể hủy đăng ký của nhân viên khác"
            );
        }
        if (registration.getStatus() != RegistrationStatus.PENDING
                && registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new IllegalStateException(
                    "Chỉ được hủy đăng ký đang chờ hoặc đã được duyệt"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cancellationDeadline = registration.getShift()
                .getStartAt()
                .minusHours(24);
        if (now.isAfter(cancellationDeadline)) {
            throw new IllegalStateException(
                    "Chỉ được hủy đăng ký trước giờ bắt đầu ca ít nhất 24 giờ"
            );
        }

        requireReason(
                request.reason(),
                "Bắt buộc nhập lý do hủy đăng ký"
        );
        UserAccount employeeUser = registration.getEmployee().getUser();
        String reason = request.reason().trim();

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledBy(employeeUser);
        registration.setCancelledAt(now);
        registration.setCancellationReason(reason);

        assignmentRepository.findByRegistration_Id(registrationId)
                .filter(this::isActiveAssignment)
                .ifPresent(assignment -> cancelAssignment(
                        assignment,
                        employeeUser,
                        now,
                        reason
                ));

        return toResponse(registration);
    }

    private Employee findEmployeeByUsername(String username) {
        return employeeRepository.findByUserUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Tài khoản chưa có hồ sơ nhân viên"
                        )
                );
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản"
                        )
                );
    }

    private void ensureEmployeeActive(Employee employee) {
        if (employee.getEmploymentStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Hồ sơ nhân viên đang không hoạt động"
            );
        }
    }

    private void validateShiftReviewable(WorkShift shift) {
        if (shift.getShiftStatus() != ShiftStatus.OPEN
                && shift.getShiftStatus() != ShiftStatus.CLOSED) {
            throw new IllegalStateException(
                    "Chỉ xử lý đăng ký khi ca đang mở hoặc đã đóng đăng ký"
            );
        }
    }

    private void ensureCapacity(WorkShift shift) {
        long assignedCount = assignmentRepository
                .countByShiftIdAndStatusIn(
                        shift.getId(),
                        ACTIVE_ASSIGNMENT_STATUSES
                );
        if (assignedCount >= shift.getRequiredStaff()) {
            throw new IllegalStateException(
                    "Ca đã đủ số lượng nhân viên"
            );
        }
    }

    private void ensureNoActiveAssignmentForSameShift(
            Long shiftId,
            Long employeeId
    ) {
        if (assignmentRepository
                .existsByShiftIdAndEmployeeIdAndStatusIn(
                        shiftId,
                        employeeId,
                        ACTIVE_ASSIGNMENT_STATUSES
                )) {
            throw new IllegalStateException(
                    "Nhân viên đã được phân công vào ca này"
            );
        }
    }

    private void ensureNoAssignmentOverlap(
            Long employeeId,
            WorkShift shift
    ) {
        if (!assignmentRepository.findOverlaps(
                employeeId,
                shift.getStartAt(),
                shift.getEndAt(),
                ACTIVE_ASSIGNMENT_STATUSES
        ).isEmpty()) {
            throw new IllegalStateException(
                    "Nhân viên bị trùng với ca đã được phân công"
            );
        }
    }

    private boolean isActiveAssignment(ShiftAssignment assignment) {
        return ACTIVE_ASSIGNMENT_STATUSES.contains(assignment.getStatus());
    }

    private void cancelAssignment(
            ShiftAssignment assignment,
            UserAccount actor,
            LocalDateTime cancelledAt,
            String reason
    ) {
        assignment.setStatus(AssignmentStatus.CANCELLED);
        assignment.setCancelledBy(actor);
        assignment.setCancelledAt(cancelledAt);
        assignment.setCancellationReason(reason);
    }

    private void requireReason(String reason, String message) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private RegistrationResponse toResponse(
            ShiftRegistration registration
    ) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getShift().getId(),
                registration.getShift().getName(),
                registration.getEmployee().getId(),
                registration.getEmployee().getUser().getFullName(),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getReviewedAt(),
                registration.getRejectionReason(),
                registration.getCancelledAt(),
                registration.getCancellationReason()
        );
    }
}
