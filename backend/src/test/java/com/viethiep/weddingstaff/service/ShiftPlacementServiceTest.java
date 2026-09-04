package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.AssignmentSource;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.CommonStatus;
import com.viethiep.weddingstaff.enumtype.ShiftRole;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftPlacementServiceTest {
    @Mock
    private ShiftAreaRepository areaRepository;
    @Mock
    private ShiftTableRepository tableRepository;
    @Mock
    private ShiftAssignmentRepository assignmentRepository;
    @Mock
    private WorkShiftRepository shiftRepository;
    @Mock
    private UserAccountRepository userRepository;

    @InjectMocks
    private ShiftPlacementService service;

    @Test
    void rejectsAreaCapacityTotalAboveShiftRequiredStaff() {
        WorkShift shift = shift(1L, 3);
        ShiftArea existing = ShiftArea.builder()
                .id(10L)
                .shift(shift)
                .name("Khu A")
                .requiredStaff(2)
                .areaStatus(CommonStatus.ACTIVE)
                .build();

        when(shiftRepository.findById(1L))
                .thenReturn(Optional.of(shift));
        when(areaRepository.existsByShiftIdAndNameIgnoreCase(
                1L,
                "Khu B"
        )).thenReturn(false);
        when(areaRepository.findAllByShiftIdOrderByNameAsc(1L))
                .thenReturn(List.of(existing));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createArea(
                        new CreateShiftAreaRequest(
                                1L,
                                "Khu B",
                                2,
                                null
                        ),
                        "admin"
                )
        );

        assertEquals(
                "Tổng nhu cầu nhân sự các khu vực không được vượt nhu cầu của ca",
                ex.getMessage()
        );
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void rejectsPlacementWhenAreaBelongsToAnotherShift() {
        WorkShift assignmentShift = shift(1L, 5);
        WorkShift otherShift = shift(2L, 5);
        ShiftAssignment assignment = assignment(30L, assignmentShift);
        ShiftArea wrongArea = ShiftArea.builder()
                .id(20L)
                .shift(otherShift)
                .name("Khu khác")
                .areaStatus(CommonStatus.ACTIVE)
                .build();

        when(assignmentRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(assignment));
        when(areaRepository.findById(20L))
                .thenReturn(Optional.of(wrongArea));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAssignmentPlacement(
                        30L,
                        new AssignmentPlacementRequest(
                                20L,
                                List.of(),
                                null
                        )
                )
        );

        assertEquals(
                "Khu vực không thuộc ca của phân công",
                ex.getMessage()
        );
    }

    @Test
    void rejectsPlacementWhenTableBelongsToAnotherArea() {
        WorkShift shift = shift(1L, 5);
        ShiftAssignment assignment = assignment(30L, shift);
        ShiftArea selectedArea = area(20L, shift, "Khu A", null);
        ShiftArea otherArea = area(21L, shift, "Khu B", null);
        ShiftTable wrongTable = table(40L, otherArea, "B01");

        when(assignmentRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(assignment));
        when(areaRepository.findById(20L))
                .thenReturn(Optional.of(selectedArea));
        when(tableRepository.findAllByIdIn(anyCollection()))
                .thenReturn(List.of(wrongTable));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAssignmentPlacement(
                        30L,
                        new AssignmentPlacementRequest(
                                20L,
                                List.of(40L),
                                null
                        )
                )
        );

        assertEquals(
                "Tất cả bàn phải thuộc khu vực đã chọn",
                ex.getMessage()
        );
    }

    @Test
    void rejectsPlacementWhenAreaCapacityIsFull() {
        WorkShift shift = shift(1L, 5);
        ShiftAssignment assignment = assignment(30L, shift);
        ShiftArea area = area(20L, shift, "Khu A", 1);

        when(assignmentRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(assignment));
        when(areaRepository.findById(20L))
                .thenReturn(Optional.of(area));
        when(assignmentRepository.countActiveByArea(
                eq(20L),
                anyCollection(),
                eq(30L)
        )).thenReturn(1L);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.updateAssignmentPlacement(
                        30L,
                        new AssignmentPlacementRequest(
                                20L,
                                List.of(),
                                null
                        )
                )
        );

        assertEquals(
                "Khu vực đã đủ số lượng nhân viên",
                ex.getMessage()
        );
    }

    @Test
    void updatesAreaTablesAndTaskForActiveAssignment() {
        WorkShift shift = shift(1L, 5);
        ShiftAssignment assignment = assignment(30L, shift);
        ShiftArea area = area(20L, shift, "Khu A", 3);
        ShiftTable a02 = table(42L, area, "A02");
        ShiftTable a01 = table(41L, area, "A01");

        when(assignmentRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(assignment));
        when(areaRepository.findById(20L))
                .thenReturn(Optional.of(area));
        when(assignmentRepository.countActiveByArea(
                eq(20L),
                anyCollection(),
                eq(30L)
        )).thenReturn(0L);
        when(tableRepository.findAllByIdIn(anyCollection()))
                .thenReturn(List.of(a02, a01));

        AssignmentPlacementResponse response =
                service.updateAssignmentPlacement(
                        30L,
                        new AssignmentPlacementRequest(
                                20L,
                                List.of(42L, 41L),
                                "Phục vụ bàn"
                        )
                );

        assertEquals(area, assignment.getShiftArea());
        assertEquals(
                List.of("A01", "A02"),
                assignment.getTables().stream()
                        .map(ShiftTable::getTableCode)
                        .toList()
        );
        assertEquals("Phục vụ bàn", assignment.getTask());

        assertEquals(20L, response.areaId());
        assertEquals("Khu A", response.areaName());
        assertEquals(List.of(41L, 42L), response.tableIds());
        assertEquals(List.of("A01", "A02"), response.tableCodes());
    }

    @Test
    void rejectsDeactivatingAreaWithActiveAssignments() {
        WorkShift shift = shift(1L, 5);
        ShiftArea area = area(20L, shift, "Khu A", 2);

        when(areaRepository.findById(20L))
                .thenReturn(Optional.of(area));
        when(assignmentRepository.countActiveByArea(
                eq(20L),
                anyCollection(),
                isNull()
        )).thenReturn(1L);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.changeAreaStatus(
                        20L,
                        new PlacementStatusRequest(
                                CommonStatus.INACTIVE
                        )
                )
        );

        assertEquals(
                "Không thể ngừng khu vực đang có phân công hoạt động",
                ex.getMessage()
        );
    }

    private WorkShift shift(Long id, int requiredStaff) {
        return WorkShift.builder()
                .id(id)
                .name("Ca " + id)
                .requiredStaff(requiredStaff)
                .shiftStatus(ShiftStatus.OPEN)
                .build();
    }

    private ShiftArea area(
            Long id,
            WorkShift shift,
            String name,
            Integer requiredStaff
    ) {
        return ShiftArea.builder()
                .id(id)
                .shift(shift)
                .name(name)
                .requiredStaff(requiredStaff)
                .areaStatus(CommonStatus.ACTIVE)
                .build();
    }

    private ShiftTable table(
            Long id,
            ShiftArea area,
            String code
    ) {
        return ShiftTable.builder()
                .id(id)
                .area(area)
                .tableCode(code)
                .tableStatus(CommonStatus.ACTIVE)
                .build();
    }

    private ShiftAssignment assignment(
            Long id,
            WorkShift shift
    ) {
        return ShiftAssignment.builder()
                .id(id)
                .shift(shift)
                .employee(Employee.builder().id(7L).build())
                .assignmentSource(AssignmentSource.DIRECT)
                .shiftRole(ShiftRole.STAFF)
                .status(AssignmentStatus.ASSIGNED)
                .assignedBy(UserAccount.builder().username("admin").build())
                .build();
    }
}
