package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {
    List<ShiftAssignment> findAllByShiftIdAndStatusIn(
            Long shiftId,
            Collection<AssignmentStatus> statuses
    );
}
