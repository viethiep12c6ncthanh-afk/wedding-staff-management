package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;
import com.viethiep.weddingstaff.enumtype.AttendanceCheckMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "attendance_check_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_check_event_action",
                        columnNames = {"attendance_id", "action"}
                )
        }
)
public class AttendanceCheckEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attendance_id", nullable = false)
    private Long attendanceId;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceCheckAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceCheckMethod method;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "distance_meters")
    private Integer distanceMeters;
}
