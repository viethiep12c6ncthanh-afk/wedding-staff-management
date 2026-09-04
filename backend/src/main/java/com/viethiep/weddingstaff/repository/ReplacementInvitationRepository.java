package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ReplacementInvitation;
import com.viethiep.weddingstaff.enumtype.ReplacementInvitationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReplacementInvitationRepository
        extends JpaRepository<ReplacementInvitation, Long> {

    boolean existsByRequestIdAndEmployeeId(Long requestId, Long employeeId);

    List<ReplacementInvitation> findAllByRequestIdAndStatusIn(
            Long requestId,
            Collection<ReplacementInvitationStatus> statuses
    );

    @Query("""
            select invitation
            from ReplacementInvitation invitation
            join fetch invitation.employee employee
            join fetch employee.user
            join fetch invitation.invitedBy
            where invitation.request.id = :requestId
            order by invitation.invitedAt desc, invitation.id desc
            """)
    List<ReplacementInvitation> findAllByRequestIdWithDetails(
            @Param("requestId") Long requestId
    );

    @Query("""
            select invitation
            from ReplacementInvitation invitation
            join fetch invitation.request request
            join fetch request.originalAssignment original
            join fetch original.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch original.employee originalEmployee
            join fetch originalEmployee.user
            join fetch invitation.employee employee
            join fetch employee.user employeeUser
            join fetch invitation.invitedBy
            left join fetch request.replacementAssignment replacement
            where employeeUser.username = :username
            order by invitation.invitedAt desc, invitation.id desc
            """)
    List<ReplacementInvitation> findAllByEmployeeUsernameWithDetails(
            @Param("username") String username
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from ReplacementInvitation invitation
            join fetch invitation.request request
            join fetch request.originalAssignment original
            join fetch original.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch original.employee originalEmployee
            join fetch originalEmployee.user
            join fetch invitation.employee employee
            join fetch employee.user
            join fetch invitation.invitedBy
            left join fetch request.replacementAssignment replacement
            where invitation.id = :id
            """)
    Optional<ReplacementInvitation> findByIdForUpdate(@Param("id") Long id);
}
