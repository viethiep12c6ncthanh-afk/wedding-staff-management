package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ShiftRegistration;
import com.viethiep.weddingstaff.enumtype.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShiftRegistrationRepository
        extends JpaRepository<ShiftRegistration, Long> {

    boolean existsByShiftIdAndEmployeeId(Long shiftId, Long employeeId);

    long countByStatus(RegistrationStatus status);

    List<ShiftRegistration> findAllByShiftIdAndStatusIn(
            Long shiftId,
            Collection<RegistrationStatus> statuses
    );

    @Query("""
            select registration
            from ShiftRegistration registration
            join fetch registration.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch registration.employee employee
            join fetch employee.user
            order by registration.createdAt desc
            """)
    List<ShiftRegistration> findAllWithDetails();

    @Query("""
            select registration
            from ShiftRegistration registration
            join fetch registration.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch registration.employee employee
            join fetch employee.user user
            where user.username = :username
            order by registration.createdAt desc
            """)
    List<ShiftRegistration> findAllByEmployeeUsername(
            @Param("username") String username
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select registration
            from ShiftRegistration registration
            join fetch registration.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch registration.employee employee
            join fetch employee.user
            where registration.id = :id
            """)
    Optional<ShiftRegistration> findByIdForUpdate(@Param("id") Long id);
}
