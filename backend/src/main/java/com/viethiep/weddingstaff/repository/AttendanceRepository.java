package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.Attendance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByAssignment_Id(Long assignmentId);

    @Query("""
            select attendance
            from Attendance attendance
            join fetch attendance.assignment assignment
            join fetch assignment.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch assignment.employee employee
            join fetch employee.user
            join fetch attendance.recordedBy
            left join fetch attendance.confirmedBy
            order by shift.startAt desc, attendance.id desc
            """)
    List<Attendance> findAllWithDetails();

    @Query("""
            select attendance
            from Attendance attendance
            join fetch attendance.assignment assignment
            join fetch assignment.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch assignment.employee employee
            join fetch employee.user user
            join fetch attendance.recordedBy
            left join fetch attendance.confirmedBy
            where user.username = :username
            order by shift.startAt desc, attendance.id desc
            """)
    List<Attendance> findAllByEmployeeUsername(
            @Param("username") String username
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attendance
            from Attendance attendance
            join fetch attendance.assignment assignment
            join fetch assignment.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch assignment.employee employee
            join fetch employee.user
            join fetch attendance.recordedBy
            left join fetch attendance.confirmedBy
            where attendance.id = :id
            """)
    Optional<Attendance> findByIdForUpdate(@Param("id") Long id);
}
