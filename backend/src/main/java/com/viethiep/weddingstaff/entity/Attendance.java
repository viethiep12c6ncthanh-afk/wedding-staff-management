package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
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
        name = "attendances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_assignment",
                        columnNames = "assignment_id"
                )
        }
)
public class Attendance extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private ShiftAssignment assignment;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 20)
    private AttendanceProcessStatus processStatus = AttendanceProcessStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_result", length = 30)
    private AttendanceResult attendanceResult;

    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer lateMinutes = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer earlyLeaveMinutes = 0;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private UserAccount recordedBy;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private UserAccount confirmedBy;

    private LocalDateTime confirmedAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal basePaySnapshot;

    @Column(precision = 12, scale = 2)
    private BigDecimal payableAmount;

    @Column(name = "payroll_policy_version", length = 40)
    private String payrollPolicyVersion;

    @Column(name = "leader_allowance_snapshot", precision = 12, scale = 2)
    private BigDecimal leaderAllowanceSnapshot;

    @Column(name = "late_deduction_snapshot", precision = 12, scale = 2)
    private BigDecimal lateDeductionSnapshot;

    @Column(name = "early_leave_deduction_snapshot", precision = 12, scale = 2)
    private BigDecimal earlyLeaveDeductionSnapshot;

    @Column(name = "overtime_minutes_snapshot")
    private Integer overtimeMinutesSnapshot;

    @Column(name = "overtime_pay_snapshot", precision = 12, scale = 2)
    private BigDecimal overtimePaySnapshot;
}
