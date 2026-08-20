package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplacementService {
    private static final EnumSet<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.CONFIRMED);
    private static final EnumSet<ReplacementRequestStatus> ACTIVE_REQUEST_STATUSES =
            EnumSet.of(ReplacementRequestStatus.PENDING, ReplacementRequestStatus.OPEN);
    private static final EnumSet<ReplacementInvitationStatus> PENDING_INVITATION_STATUS =
            EnumSet.of(ReplacementInvitationStatus.PENDING);

    private final ReplacementRequestRepository requestRepository;
    private final ReplacementInvitationRepository invitationRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final WorkShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public List<ReplacementRequestResponse> findAllRequests() {
        return requestRepository.findAllWithDetails().stream()
                .map(this::toRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReplacementRequestResponse> findMyRequests(String username) {
        return requestRepository.findAllByEmployeeUsernameWithDetails(username)
                .stream()
                .map(this::toRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReplacementInvitationResponse> findMyInvitations(String username) {
        return invitationRepository.findAllByEmployeeUsernameWithDetails(username)
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReplacementEligibleAssignmentResponse> findMyEligibleAssignments(
            String username
    ) {
        Employee employee = findEmployeeByUsername(username);
        LocalDateTime now = LocalDateTime.now();

        return assignmentRepository.findAllWithDetails().stream()
                .filter(assignment -> assignment.getEmployee().getId().equals(employee.getId()))
                .filter(this::isActiveAssignment)
                .filter(assignment -> isReplacementRequestable(assignment, now))
                .filter(assignment -> !requestRepository
                        .existsByOriginalAssignmentIdAndStatusIn(
                                assignment.getId(),
                                ACTIVE_REQUEST_STATUSES
                        ))
                .map(this::toEligibleAssignmentResponse)
                .toList();
    }

    @Transactional
    public ReplacementRequestResponse createRequest(
            CreateReplacementRequest request,
            String username
    ) {
        ShiftAssignment assignment = assignmentRepository
                .findByIdForUpdate(request.assignmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy phân công")
                );
        Employee requesterEmployee = findEmployeeByUsername(username);

        if (!assignment.getEmployee().getId().equals(requesterEmployee.getId())) {
            throw new IllegalStateException(
                    "Chỉ được gửi yêu cầu thay ca cho phân công của chính bạn"
            );
        }
        if (!isActiveAssignment(assignment)) {
            throw new IllegalStateException(
                    "Chỉ được yêu cầu thay ca cho phân công đang hoạt động"
            );
        }
        ensureBeforeShiftStart(assignment.getShift());
        ensureShiftCanUseReplacement(assignment.getShift());

        if (requestRepository.existsByOriginalAssignmentIdAndStatusIn(
                assignment.getId(),
                ACTIVE_REQUEST_STATUSES
        )) {
            throw new IllegalStateException(
                    "Phân công này đã có yêu cầu thay ca đang xử lý"
            );
        }

        UserAccount requester = findUser(username);
        ReplacementRequest entity = ReplacementRequest.builder()
                .originalAssignment(assignment)
                .requestedBy(requester)
                .reason(request.reason().trim())
                .status(ReplacementRequestStatus.PENDING)
                .build();

        return toRequestResponse(requestRepository.save(entity));
    }

    @Transactional
    public ReplacementRequestResponse reviewRequest(
            Long requestId,
            ReviewReplacementRequest review,
            String reviewerUsername
    ) {
        ReplacementRequest request = findRequestForUpdate(requestId);
        if (request.getStatus() != ReplacementRequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu thay ca đã được xử lý");
        }

        UserAccount reviewer = findUser(reviewerUsername);
        LocalDateTime now = LocalDateTime.now();
        request.setReviewedBy(reviewer);
        request.setReviewedAt(now);
        request.setReviewNote(trimToNull(review.reviewNote()));

        if (!Boolean.TRUE.equals(review.approved())) {
            if (review.reviewNote() == null || review.reviewNote().isBlank()) {
                throw new IllegalArgumentException(
                        "Bắt buộc nhập lý do từ chối yêu cầu thay ca"
                );
            }
            request.setStatus(ReplacementRequestStatus.REJECTED);
            request.setClosedAt(now);
            request.setClosedReason(review.reviewNote().trim());
            return toRequestResponse(request);
        }

        ShiftAssignment original = assignmentRepository
                .findByIdForUpdate(request.getOriginalAssignment().getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy phân công gốc")
                );
        if (!isActiveAssignment(original)) {
            throw new IllegalStateException(
                    "Phân công gốc không còn hoạt động để duyệt thay ca"
            );
        }
        ensureBeforeShiftStart(original.getShift());
        ensureShiftCanUseReplacement(original.getShift());

        String cancellationReason = "Yêu cầu thay ca #" + request.getId()
                + ": " + request.getReason();
        cancelOriginalAssignment(original, reviewer, now, cancellationReason);
        request.setStatus(ReplacementRequestStatus.OPEN);
        return toRequestResponse(request);
    }

    @Transactional
    public ReplacementRequestResponse inviteEmployee(
            Long requestId,
            InviteReplacementRequest invite,
            String inviterUsername
    ) {
        ReplacementRequest request = findRequestForUpdate(requestId);
        ensureRequestOpen(request);

        WorkShift shift = shiftRepository
                .findByIdForUpdate(request.getOriginalAssignment().getShift().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca"));
        ensureBeforeShiftStart(shift);
        ensureShiftCanUseReplacement(shift);

        Employee candidate = employeeRepository.findByIdWithUser(invite.employeeId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy nhân viên")
                );
        ensureCandidateEligible(request, candidate, shift);

        if (invitationRepository.existsByRequestIdAndEmployeeId(
                requestId,
                candidate.getId()
        )) {
            throw new IllegalStateException(
                    "Nhân viên này đã từng được mời cho yêu cầu thay ca"
            );
        }

        UserAccount inviter = findUser(inviterUsername);
        ReplacementInvitation invitation = ReplacementInvitation.builder()
                .request(request)
                .employee(candidate)
                .status(ReplacementInvitationStatus.PENDING)
                .invitedBy(inviter)
                .invitedAt(LocalDateTime.now())
                .build();
        invitationRepository.save(invitation);

        return toRequestResponse(request);
    }

    @Transactional
    public ReplacementInvitationResponse respondInvitation(
            Long invitationId,
            RespondReplacementInvitationRequest response,
            String username
    ) {
        ReplacementInvitation invitation = invitationRepository
                .findByIdForUpdate(invitationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy lời mời thay ca")
                );

        if (!invitation.getEmployee().getUser().getUsername().equals(username)) {
            throw new IllegalStateException(
                    "Không thể phản hồi lời mời của nhân viên khác"
            );
        }
        if (invitation.getStatus() != ReplacementInvitationStatus.PENDING) {
            throw new IllegalStateException("Lời mời thay ca đã được phản hồi");
        }

        ReplacementRequest request = findRequestForUpdate(invitation.getRequest().getId());
        ensureRequestOpen(request);
        LocalDateTime now = LocalDateTime.now();
        invitation.setRespondedAt(now);
        invitation.setResponseNote(trimToNull(response.responseNote()));

        if (!Boolean.TRUE.equals(response.accepted())) {
            invitation.setStatus(ReplacementInvitationStatus.DECLINED);
            return toInvitationResponse(invitation);
        }

        WorkShift shift = shiftRepository
                .findByIdForUpdate(request.getOriginalAssignment().getShift().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca"));
        ensureBeforeShiftStart(shift);
        ensureShiftCanUseReplacement(shift);
        ensureCandidateEligible(request, invitation.getEmployee(), shift);
        ensureCapacity(shift);

        ShiftAssignment original = request.getOriginalAssignment();
        ShiftAssignment replacement = ShiftAssignment.builder()
                .shift(shift)
                .employee(invitation.getEmployee())
                .registration(null)
                .assignmentSource(AssignmentSource.REPLACEMENT)
                .shiftRole(original.getShiftRole())
                .area(original.getArea())
                .task(original.getTask())
                .status(AssignmentStatus.ASSIGNED)
                .assignedBy(invitation.getInvitedBy())
                .build();
        replacement = assignmentRepository.save(replacement);

        invitation.setStatus(ReplacementInvitationStatus.ACCEPTED);
        request.setReplacementAssignment(replacement);
        request.setStatus(ReplacementRequestStatus.FILLED);
        request.setFilledAt(now);

        cancelOtherPendingInvitations(request.getId(), invitation.getId(), now);
        return toInvitationResponse(invitation);
    }

    @Transactional
    public void cancelForShift(Long shiftId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        List<ReplacementRequest> activeRequests = requestRepository
                .findAllByOriginalAssignmentShiftIdAndStatusIn(
                        shiftId,
                        ACTIVE_REQUEST_STATUSES
                );

        for (ReplacementRequest request : activeRequests) {
            request.setStatus(ReplacementRequestStatus.CANCELLED);
            request.setClosedAt(now);
            request.setClosedReason(reason);
            invitationRepository.findAllByRequestIdAndStatusIn(
                    request.getId(),
                    PENDING_INVITATION_STATUS
            ).forEach(invitation -> {
                invitation.setStatus(ReplacementInvitationStatus.CANCELLED);
                invitation.setRespondedAt(now);
                invitation.setResponseNote("Yêu cầu thay ca đã đóng vì ca không còn hoạt động");
            });
        }
    }

    private void cancelOtherPendingInvitations(
            Long requestId,
            Long acceptedInvitationId,
            LocalDateTime now
    ) {
        invitationRepository.findAllByRequestIdAndStatusIn(
                requestId,
                PENDING_INVITATION_STATUS
        ).stream()
                .filter(item -> !item.getId().equals(acceptedInvitationId))
                .forEach(item -> {
                    item.setStatus(ReplacementInvitationStatus.CANCELLED);
                    item.setRespondedAt(now);
                    item.setResponseNote("Yêu cầu đã có nhân viên nhận thay ca");
                });
    }

    private void ensureCandidateEligible(
            ReplacementRequest request,
            Employee candidate,
            WorkShift shift
    ) {
        if (candidate.getEmploymentStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalStateException("Hồ sơ nhân viên đang không hoạt động");
        }
        if (candidate.getId().equals(request.getOriginalAssignment().getEmployee().getId())) {
            throw new IllegalStateException("Không thể mời chính nhân viên đang xin thay ca");
        }
        if (assignmentRepository.existsByShiftIdAndEmployeeIdAndStatusIn(
                shift.getId(),
                candidate.getId(),
                ACTIVE_ASSIGNMENT_STATUSES
        )) {
            throw new IllegalStateException("Nhân viên đã được phân công vào ca này");
        }
        if (!assignmentRepository.findOverlaps(
                candidate.getId(),
                shift.getStartAt(),
                shift.getEndAt(),
                ACTIVE_ASSIGNMENT_STATUSES
        ).isEmpty()) {
            throw new IllegalStateException("Nhân viên bị trùng với ca đã được phân công");
        }
    }

    private void ensureCapacity(WorkShift shift) {
        long activeCount = assignmentRepository.countByShiftIdAndStatusIn(
                shift.getId(),
                ACTIVE_ASSIGNMENT_STATUSES
        );
        if (activeCount >= shift.getRequiredStaff()) {
            throw new IllegalStateException(
                    "Ca đã đủ nhân sự; không thể nhận thêm người thay"
            );
        }
    }

    private void cancelOriginalAssignment(
            ShiftAssignment assignment,
            UserAccount actor,
            LocalDateTime now,
            String reason
    ) {
        assignment.setStatus(AssignmentStatus.CANCELLED);
        assignment.setCancelledBy(actor);
        assignment.setCancelledAt(now);
        assignment.setCancellationReason(reason);

        ShiftRegistration registration = assignment.getRegistration();
        if (registration != null && registration.getStatus() == RegistrationStatus.APPROVED) {
            registration.setStatus(RegistrationStatus.CANCELLED);
            registration.setCancelledBy(actor);
            registration.setCancelledAt(now);
            registration.setCancellationReason(reason);
        }
    }

    private void ensureBeforeShiftStart(WorkShift shift) {
        if (!LocalDateTime.now().isBefore(shift.getStartAt())) {
            throw new IllegalStateException(
                    "Không thể xử lý thay ca khi ca đã bắt đầu"
            );
        }
    }

    private void ensureShiftCanUseReplacement(WorkShift shift) {
        if (shift.getShiftStatus() != ShiftStatus.OPEN
                && shift.getShiftStatus() != ShiftStatus.CLOSED) {
            throw new IllegalStateException(
                    "Chỉ xử lý thay ca khi ca đang mở hoặc đã đóng đăng ký"
            );
        }
    }

    private void ensureRequestOpen(ReplacementRequest request) {
        if (request.getStatus() != ReplacementRequestStatus.OPEN) {
            throw new IllegalStateException("Yêu cầu thay ca không ở trạng thái đang tìm người thay");
        }
    }

    private boolean isActiveAssignment(ShiftAssignment assignment) {
        return ACTIVE_ASSIGNMENT_STATUSES.contains(assignment.getStatus());
    }

    private boolean isReplacementRequestable(
            ShiftAssignment assignment,
            LocalDateTime now
    ) {
        return now.isBefore(assignment.getShift().getStartAt())
                && (assignment.getShift().getShiftStatus() == ShiftStatus.OPEN
                || assignment.getShift().getShiftStatus() == ShiftStatus.CLOSED);
    }

    private ReplacementRequest findRequestForUpdate(Long requestId) {
        return requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy yêu cầu thay ca")
                );
    }

    private Employee findEmployeeByUsername(String username) {
        return employeeRepository.findByUserUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Tài khoản chưa có hồ sơ nhân viên")
                );
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy tài khoản")
                );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ReplacementEligibleAssignmentResponse toEligibleAssignmentResponse(
            ShiftAssignment assignment
    ) {
        WorkShift shift = assignment.getShift();
        return new ReplacementEligibleAssignmentResponse(
                assignment.getId(),
                shift.getId(),
                shift.getName(),
                shift.getEvent().getName(),
                shift.getEvent().getVenue().getName(),
                shift.getStartAt(),
                shift.getEndAt(),
                assignment.getShiftRole(),
                assignment.getArea(),
                assignment.getTask(),
                assignment.getStatus()
        );
    }

    private ReplacementRequestResponse toRequestResponse(ReplacementRequest request) {
        ShiftAssignment original = request.getOriginalAssignment();
        WorkShift shift = original.getShift();
        ShiftAssignment replacement = request.getReplacementAssignment();
        List<ReplacementInvitationResponse> invitations = request.getId() == null
                ? List.of()
                : invitationRepository.findAllByRequestIdWithDetails(request.getId())
                        .stream()
                        .map(this::toInvitationResponse)
                        .toList();

        return new ReplacementRequestResponse(
                request.getId(),
                original.getId(),
                shift.getId(),
                shift.getName(),
                shift.getEvent().getName(),
                shift.getEvent().getVenue().getName(),
                shift.getStartAt(),
                shift.getEndAt(),
                original.getEmployee().getId(),
                original.getEmployee().getUser().getFullName(),
                request.getRequestedBy().getUsername(),
                request.getReason(),
                request.getStatus(),
                request.getReviewedBy() == null ? null : request.getReviewedBy().getUsername(),
                request.getReviewedAt(),
                request.getReviewNote(),
                replacement == null ? null : replacement.getId(),
                replacement == null ? null : replacement.getEmployee().getId(),
                replacement == null ? null : replacement.getEmployee().getUser().getFullName(),
                request.getFilledAt(),
                request.getClosedAt(),
                request.getClosedReason(),
                request.getCreatedAt(),
                invitations
        );
    }

    private ReplacementInvitationResponse toInvitationResponse(
            ReplacementInvitation invitation
    ) {
        ReplacementRequest request = invitation.getRequest();
        ShiftAssignment original = request.getOriginalAssignment();
        WorkShift shift = original.getShift();
        ShiftAssignment replacement = request.getReplacementAssignment();

        return new ReplacementInvitationResponse(
                invitation.getId(),
                request.getId(),
                invitation.getEmployee().getId(),
                invitation.getEmployee().getUser().getFullName(),
                invitation.getStatus(),
                invitation.getInvitedBy().getUsername(),
                invitation.getInvitedAt(),
                invitation.getRespondedAt(),
                invitation.getResponseNote(),
                shift.getId(),
                shift.getName(),
                shift.getEvent().getName(),
                shift.getEvent().getVenue().getName(),
                shift.getStartAt(),
                shift.getEndAt(),
                original.getEmployee().getUser().getFullName(),
                request.getStatus(),
                replacement == null ? null : replacement.getId()
        );
    }
}
