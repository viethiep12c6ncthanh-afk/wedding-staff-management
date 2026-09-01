package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ShiftTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ShiftTableRepository extends JpaRepository<ShiftTable, Long> {
    List<ShiftTable> findAllByAreaIdOrderByTableCodeAsc(Long areaId);

    List<ShiftTable> findAllByIdIn(Collection<Long> ids);

    boolean existsByAreaIdAndTableCodeIgnoreCase(Long areaId, String tableCode);

    boolean existsByAreaIdAndTableCodeIgnoreCaseAndIdNot(
            Long areaId,
            String tableCode,
            Long id
    );
}
