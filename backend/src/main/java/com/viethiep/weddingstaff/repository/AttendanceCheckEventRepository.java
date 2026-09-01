package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.AttendanceCheckEvent;
import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceCheckEventRepository
        extends JpaRepository<AttendanceCheckEvent, Long> {

    boolean existsByAttendanceIdAndAction(
            Long attendanceId,
            AttendanceCheckAction action
    );
}
