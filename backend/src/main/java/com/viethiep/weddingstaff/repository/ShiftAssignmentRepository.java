package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.ShiftRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShiftAssignmentRepository
        extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findAllByShiftIdAndStatusIn(
            Long shiftId,
            Collection<AssignmentStatus> statuses
    );

    long countByShiftIdAndStatusIn(
            Long shiftId,
            Collection<AssignmentStatus> statuses
    );

    boolean existsByShiftIdAndEmployeeIdAndStatusIn(
            Long shiftId,
            Long employeeId,
            Collection<AssignmentStatus> statuses
    );

    @Query("""
            select count(assignment)
            from ShiftAssignment assignment
            where assignment.shift.id = :shiftId
              and assignment.employee.user.username = :username
              and assignment.shiftRole = :shiftRole
              and assignment.status in :statuses
            """)
    long countLeaderAssignments(
            @Param("shiftId") Long shiftId,
            @Param("username") String username,
            @Param("shiftRole") ShiftRole shiftRole,
            @Param("statuses") Collection<AssignmentStatus> statuses
    );

    Optional<ShiftAssignment> findByRegistration_Id(Long registrationId);

    @Query("""
            select assignment
            from ShiftAssignment assignment
            join assignment.shift shift
            where assignment.employee.id = :employeeId
              and assignment.status in :statuses
              and shift.startAt < :endAt
              and shift.endAt > :startAt
            """)
    List<ShiftAssignment> findOverlaps(
            @Param("employeeId") Long employeeId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("statuses") Collection<AssignmentStatus> statuses
    );

    @Query("""
            select assignment
            from ShiftAssignment assignment
            join fetch assignment.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch assignment.employee employee
            join fetch employee.user
            join fetch assignment.assignedBy
            left join fetch assignment.registration
            order by assignment.createdAt desc
            """)
    List<ShiftAssignment> findAllWithDetails();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select assignment
            from ShiftAssignment assignment
            join fetch assignment.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch assignment.employee employee
            join fetch employee.user
            join fetch assignment.assignedBy
            left join fetch assignment.registration
            where assignment.id = :id
            """)
    Optional<ShiftAssignment> findByIdForUpdate(@Param("id") Long id);
}
