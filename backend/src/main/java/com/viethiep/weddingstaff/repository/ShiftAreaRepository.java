package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ShiftArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftAreaRepository extends JpaRepository<ShiftArea, Long> {
    List<ShiftArea> findAllByShiftIdOrderByNameAsc(Long shiftId);

    boolean existsByShiftIdAndNameIgnoreCase(Long shiftId, String name);

    boolean existsByShiftIdAndNameIgnoreCaseAndIdNot(
            Long shiftId,
            String name,
            Long id
    );
}
