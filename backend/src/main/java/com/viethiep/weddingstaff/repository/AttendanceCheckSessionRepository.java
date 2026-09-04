package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.AttendanceCheckSession;
import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceCheckSessionRepository
        extends JpaRepository<AttendanceCheckSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AttendanceCheckSession session
            join fetch session.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch session.createdBy
            left join fetch session.revokedBy
            where session.tokenHash = :tokenHash
            """)
    Optional<AttendanceCheckSession> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AttendanceCheckSession session
            join fetch session.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch session.createdBy
            left join fetch session.revokedBy
            where shift.id = :shiftId
              and session.action = :action
              and session.revokedAt is null
              and session.expiresAt > :now
            order by session.id desc
            """)
    List<AttendanceCheckSession> findActiveForUpdate(
            @Param("shiftId") Long shiftId,
            @Param("action") AttendanceCheckAction action,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AttendanceCheckSession session
            join fetch session.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch session.createdBy
            left join fetch session.revokedBy
            where session.id = :id
            """)
    Optional<AttendanceCheckSession> findByIdForUpdate(@Param("id") Long id);
}
