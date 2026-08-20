package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ReplacementRequest;
import com.viethiep.weddingstaff.enumtype.ReplacementRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReplacementRequestRepository
        extends JpaRepository<ReplacementRequest, Long> {

    boolean existsByOriginalAssignmentIdAndStatusIn(
            Long originalAssignmentId,
            Collection<ReplacementRequestStatus> statuses
    );

    List<ReplacementRequest> findAllByOriginalAssignmentShiftIdAndStatusIn(
            Long shiftId,
            Collection<ReplacementRequestStatus> statuses
    );

    @Query("""
            select request
            from ReplacementRequest request
            join fetch request.originalAssignment original
            join fetch original.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch original.employee originalEmployee
            join fetch originalEmployee.user
            join fetch request.requestedBy
            left join fetch request.reviewedBy
            left join fetch request.replacementAssignment replacement
            left join fetch replacement.employee replacementEmployee
            left join fetch replacementEmployee.user
            order by request.createdAt desc
            """)
    List<ReplacementRequest> findAllWithDetails();

    @Query("""
            select request
            from ReplacementRequest request
            join fetch request.originalAssignment original
            join fetch original.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch original.employee originalEmployee
            join fetch originalEmployee.user originalUser
            join fetch request.requestedBy
            left join fetch request.reviewedBy
            left join fetch request.replacementAssignment replacement
            left join fetch replacement.employee replacementEmployee
            left join fetch replacementEmployee.user
            where originalUser.username = :username
            order by request.createdAt desc
            """)
    List<ReplacementRequest> findAllByEmployeeUsernameWithDetails(
            @Param("username") String username
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request
            from ReplacementRequest request
            join fetch request.originalAssignment original
            join fetch original.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch original.employee originalEmployee
            join fetch originalEmployee.user
            left join fetch original.registration
            join fetch request.requestedBy
            left join fetch request.reviewedBy
            left join fetch request.replacementAssignment replacement
            left join fetch replacement.employee replacementEmployee
            left join fetch replacementEmployee.user
            where request.id = :id
            """)
    Optional<ReplacementRequest> findByIdForUpdate(@Param("id") Long id);
}
