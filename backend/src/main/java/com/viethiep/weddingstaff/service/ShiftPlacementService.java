package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.CommonStatus;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShiftPlacementService {
    private static final EnumSet<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.CONFIRMED);

    private final ShiftAreaRepository areaRepository;
    private final ShiftTableRepository tableRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final WorkShiftRepository shiftRepository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public List<ShiftAreaResponse> findAreas(Long shiftId) {
        findShift(shiftId);
        return areaRepository.findAllByShiftIdOrderByNameAsc(shiftId).stream()
                .map(this::toAreaResponse)
                .toList();
    }

    @Transactional
    public ShiftAreaResponse createArea(
            CreateShiftAreaRequest request,
            String username
    ) {
        WorkShift shift = findShift(request.shiftId());
        ensureShiftPlacementEditable(shift);

        String name = request.name().trim();
        if (areaRepository.existsByShiftIdAndNameIgnoreCase(shift.getId(), name)) {
            throw new IllegalStateException(
                    "Ca đã có khu vực cùng tên"
            );
        }

        validateConfiguredAreaCapacity(
                shift,
                null,
                request.requiredStaff(),
                CommonStatus.ACTIVE
        );

        ShiftArea area = ShiftArea.builder()
                .shift(shift)
                .name(name)
                .requiredStaff(request.requiredStaff())
                .description(trimToNull(request.description()))
                .areaStatus(CommonStatus.ACTIVE)
                .createdBy(findUser(username))
                .build();

        return toAreaResponse(areaRepository.save(area));
    }

    @Transactional
    public ShiftAreaResponse updateArea(
            Long areaId,
            UpdateShiftAreaRequest request
    ) {
        ShiftArea area = findArea(areaId);
        WorkShift shift = area.getShift();
        ensureShiftPlacementEditable(shift);

        String name = request.name().trim();
        if (areaRepository.existsByShiftIdAndNameIgnoreCaseAndIdNot(
                shift.getId(),
                name,
                areaId
        )) {
            throw new IllegalStateException(
                    "Ca đã có khu vực cùng tên"
            );
        }

        long assignedCount = assignmentRepository.countActiveByArea(
                areaId,
                ACTIVE_ASSIGNMENT_STATUSES,
                null
        );
        if (request.requiredStaff() != null
                && assignedCount > request.requiredStaff()) {
            throw new IllegalStateException(
                    "Sức chứa khu vực nhỏ hơn số nhân viên đang được phân vào khu vực"
            );
        }

        validateConfiguredAreaCapacity(
                shift,
                areaId,
                request.requiredStaff(),
                area.getAreaStatus()
        );

        area.setName(name);
        area.setRequiredStaff(request.requiredStaff());
        area.setDescription(trimToNull(request.description()));

        return toAreaResponse(area);
    }

    @Transactional
    public ShiftAreaResponse changeAreaStatus(
            Long areaId,
            PlacementStatusRequest request
    ) {
        ShiftArea area = findArea(areaId);
        ensureShiftPlacementEditable(area.getShift());

        if (request.status() == CommonStatus.INACTIVE) {
            long activeCount = assignmentRepository.countActiveByArea(
                    areaId,
                    ACTIVE_ASSIGNMENT_STATUSES,
                    null
            );
            if (activeCount > 0) {
                throw new IllegalStateException(
                        "Không thể ngừng khu vực đang có phân công hoạt động"
                );
            }
        } else {
            validateConfiguredAreaCapacity(
                    area.getShift(),
                    areaId,
                    area.getRequiredStaff(),
                    CommonStatus.ACTIVE
            );
        }

        area.setAreaStatus(request.status());
        return toAreaResponse(area);
    }

    @Transactional
    public ShiftTableResponse createTable(
            CreateShiftTableRequest request,
            String username
    ) {
        ShiftArea area = findArea(request.areaId());
        ensureShiftPlacementEditable(area.getShift());

        if (area.getAreaStatus() != CommonStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Chỉ tạo bàn trong khu vực đang hoạt động"
            );
        }

        String tableCode = normalizeTableCode(request.tableCode());
        if (tableRepository.existsByAreaIdAndTableCodeIgnoreCase(
                area.getId(),
                tableCode
        )) {
            throw new IllegalStateException(
                    "Khu vực đã có mã bàn này"
            );
        }

        ShiftTable table = ShiftTable.builder()
                .area(area)
                .tableCode(tableCode)
                .displayName(trimToNull(request.displayName()))
                .note(trimToNull(request.note()))
                .tableStatus(CommonStatus.ACTIVE)
                .createdBy(findUser(username))
                .build();

        return toTableResponse(tableRepository.save(table));
    }

    @Transactional
    public ShiftTableResponse updateTable(
            Long tableId,
            UpdateShiftTableRequest request
    ) {
        ShiftTable table = findTable(tableId);
        ensureShiftPlacementEditable(table.getArea().getShift());

        String tableCode = normalizeTableCode(request.tableCode());
        if (tableRepository.existsByAreaIdAndTableCodeIgnoreCaseAndIdNot(
                table.getArea().getId(),
                tableCode,
                tableId
        )) {
            throw new IllegalStateException(
                    "Khu vực đã có mã bàn này"
            );
        }

        table.setTableCode(tableCode);
        table.setDisplayName(trimToNull(request.displayName()));
        table.setNote(trimToNull(request.note()));

        return toTableResponse(table);
    }

    @Transactional
    public ShiftTableResponse changeTableStatus(
            Long tableId,
            PlacementStatusRequest request
    ) {
        ShiftTable table = findTable(tableId);
        ensureShiftPlacementEditable(table.getArea().getShift());

        if (request.status() == CommonStatus.INACTIVE) {
            long activeCount = assignmentRepository.countActiveByTable(
                    tableId,
                    ACTIVE_ASSIGNMENT_STATUSES
            );
            if (activeCount > 0) {
                throw new IllegalStateException(
                        "Không thể ngừng bàn đang có phân công hoạt động"
                );
            }
        } else if (table.getArea().getAreaStatus() != CommonStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Không thể kích hoạt bàn khi khu vực đang ngừng hoạt động"
            );
        }

        table.setTableStatus(request.status());
        return toTableResponse(table);
    }

    @Transactional
    public AssignmentPlacementResponse updateAssignmentPlacement(
            Long assignmentId,
            AssignmentPlacementRequest request
    ) {
        ShiftAssignment assignment = assignmentRepository
                .findByIdForUpdate(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy phân công")
                );

        if (!ACTIVE_ASSIGNMENT_STATUSES.contains(assignment.getStatus())) {
            throw new IllegalStateException(
                    "Chỉ được điều phối khu vực/bàn cho phân công đang hoạt động"
            );
        }
        ensureShiftPlacementEditable(assignment.getShift());

        Set<Long> requestedTableIds = request.tableIds() == null
                ? Set.of()
                : new LinkedHashSet<>(request.tableIds());

        if (request.areaId() == null) {
            if (!requestedTableIds.isEmpty()) {
                throw new IllegalArgumentException(
                        "Không thể chọn bàn khi chưa chọn khu vực"
                );
            }
            assignment.setShiftArea(null);
            assignment.getTables().clear();
            assignment.setTask(trimToNull(request.task()));
            return toPlacementResponse(assignment);
        }

        ShiftArea area = findArea(request.areaId());
        if (!area.getShift().getId().equals(assignment.getShift().getId())) {
            throw new IllegalArgumentException(
                    "Khu vực không thuộc ca của phân công"
            );
        }
        if (area.getAreaStatus() != CommonStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Khu vực đang ngừng hoạt động"
            );
        }

        if (area.getRequiredStaff() != null) {
            long otherActiveAssignments = assignmentRepository.countActiveByArea(
                    area.getId(),
                    ACTIVE_ASSIGNMENT_STATUSES,
                    assignment.getId()
            );
            if (otherActiveAssignments >= area.getRequiredStaff()) {
                throw new IllegalStateException(
                        "Khu vực đã đủ số lượng nhân viên"
                );
            }
        }

        List<ShiftTable> tables = requestedTableIds.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(tableRepository.findAllByIdIn(requestedTableIds));

        if (tables.size() != requestedTableIds.size()) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy một hoặc nhiều bàn đã chọn"
            );
        }

        for (ShiftTable table : tables) {
            if (!table.getArea().getId().equals(area.getId())) {
                throw new IllegalArgumentException(
                        "Tất cả bàn phải thuộc khu vực đã chọn"
                );
            }
            if (table.getTableStatus() != CommonStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Không thể phân công vào bàn đang ngừng hoạt động"
                );
            }
        }

        tables.sort(Comparator.comparing(
                ShiftTable::getTableCode,
                String.CASE_INSENSITIVE_ORDER
        ));

        assignment.setShiftArea(area);
        assignment.setTables(new LinkedHashSet<>(tables));
        assignment.setTask(trimToNull(request.task()));

        return toPlacementResponse(assignment);
    }

    private ShiftAreaResponse toAreaResponse(ShiftArea area) {
        List<ShiftTableResponse> tables = tableRepository
                .findAllByAreaIdOrderByTableCodeAsc(area.getId())
                .stream()
                .map(this::toTableResponse)
                .toList();

        long assignedStaffCount = assignmentRepository.countActiveByArea(
                area.getId(),
                ACTIVE_ASSIGNMENT_STATUSES,
                null
        );

        return new ShiftAreaResponse(
                area.getId(),
                area.getShift().getId(),
                area.getShift().getName(),
                area.getName(),
                area.getRequiredStaff(),
                area.getDescription(),
                area.getAreaStatus(),
                assignedStaffCount,
                tables
        );
    }

    private ShiftTableResponse toTableResponse(ShiftTable table) {
        return new ShiftTableResponse(
                table.getId(),
                table.getArea().getId(),
                table.getTableCode(),
                table.getDisplayName(),
                table.getNote(),
                table.getTableStatus()
        );
    }

    private AssignmentPlacementResponse toPlacementResponse(
            ShiftAssignment assignment
    ) {
        List<ShiftTable> tables = assignment.getTables().stream()
                .sorted(Comparator.comparing(
                        ShiftTable::getTableCode,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        return new AssignmentPlacementResponse(
                assignment.getId(),
                assignment.getShiftArea() == null
                        ? null
                        : assignment.getShiftArea().getId(),
                assignment.getShiftArea() == null
                        ? null
                        : assignment.getShiftArea().getName(),
                tables.stream().map(ShiftTable::getId).toList(),
                tables.stream().map(ShiftTable::getTableCode).toList(),
                assignment.getTask()
        );
    }

    private void validateConfiguredAreaCapacity(
            WorkShift shift,
            Long excludedAreaId,
            Integer candidateRequiredStaff,
            CommonStatus candidateStatus
    ) {
        int configuredTotal = areaRepository
                .findAllByShiftIdOrderByNameAsc(shift.getId())
                .stream()
                .filter(area ->
                        excludedAreaId == null
                                || !area.getId().equals(excludedAreaId)
                )
                .filter(area -> area.getAreaStatus() == CommonStatus.ACTIVE)
                .map(ShiftArea::getRequiredStaff)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        if (candidateStatus == CommonStatus.ACTIVE
                && candidateRequiredStaff != null) {
            configuredTotal += candidateRequiredStaff;
        }

        if (configuredTotal > shift.getRequiredStaff()) {
            throw new IllegalStateException(
                    "Tổng nhu cầu nhân sự các khu vực không được vượt nhu cầu của ca"
            );
        }
    }

    private void ensureShiftPlacementEditable(WorkShift shift) {
        if (shift.getShiftStatus() == ShiftStatus.COMPLETED
                || shift.getShiftStatus() == ShiftStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Không thể thay đổi khu vực/bàn của ca đã hoàn thành hoặc đã hủy"
            );
        }
    }

    private WorkShift findShift(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy ca")
                );
    }

    private ShiftArea findArea(Long areaId) {
        return areaRepository.findById(areaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy khu vực")
                );
    }

    private ShiftTable findTable(Long tableId) {
        return tableRepository.findById(tableId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy bàn")
                );
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy tài khoản")
                );
    }

    private String normalizeTableCode(String value) {
        return value.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
