package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
    long countByShiftStatus(ShiftStatus shiftStatus);

    @Query("""
            select shift
            from WorkShift shift
            join fetch shift.event event
            join fetch event.venue
            order by shift.startAt desc
            """)
    List<WorkShift> findAllWithEventAndVenue();

    List<WorkShift> findAllByEventId(Long eventId);

    @Query("""
            select shift
            from WorkShift shift
            join fetch shift.event event
            join fetch event.venue venue
            where shift.startAt < :endAt
              and shift.endAt > :startAt
              and shift.shiftStatus in :statuses
              and (:venueId is null or venue.id = :venueId)
            order by shift.startAt asc, shift.id asc
            """)
    List<WorkShift> findForCoordinationWindow(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("venueId") Long venueId,
            @Param("statuses") Collection<ShiftStatus> statuses
    );

    @Query("""
            select shift
            from WorkShift shift
            join fetch shift.event event
            join fetch event.venue venue
            where shift.startAt >= :fromAt
              and shift.startAt < :toExclusive
              and shift.shiftStatus in :statuses
              and (:venueId is null or venue.id = :venueId)
            order by shift.startAt asc, shift.id asc
            """)
    List<WorkShift> findForDashboardRange(
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("venueId") Long venueId,
            @Param("statuses") Collection<ShiftStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select shift
            from WorkShift shift
            join fetch shift.event event
            join fetch event.venue
            where shift.id = :id
            """)
    Optional<WorkShift> findByIdForUpdate(@Param("id") Long id);
}
