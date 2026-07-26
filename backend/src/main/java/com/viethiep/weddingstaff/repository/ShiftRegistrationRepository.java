package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ShiftRegistration;
import com.viethiep.weddingstaff.enumtype.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShiftRegistrationRepository extends JpaRepository<ShiftRegistration, Long> {
    boolean existsByShiftIdAndEmployeeId(Long shiftId, Long employeeId);
    long countByShiftIdAndStatus(Long shiftId, RegistrationStatus status);

    @Query("""
        select r from ShiftRegistration r
        where r.employee.id = :employeeId
          and r.status = com.viethiep.weddingstaff.enumtype.RegistrationStatus.APPROVED
          and r.shift.startAt < :endAt
          and r.shift.endAt > :startAt
    """)
    List<ShiftRegistration> findApprovedOverlaps(
            @Param("employeeId") Long employeeId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);
}
