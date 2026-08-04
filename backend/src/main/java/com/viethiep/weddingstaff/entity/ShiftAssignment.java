package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.AssignmentSource;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.ShiftRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "shift_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_assignment_registration",
                        columnNames = "registration_id"
                )
        }
)
public class ShiftAssignment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", unique = true)
    private ShiftRegistration registration;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_source", nullable = false, length = 20)
    private AssignmentSource assignmentSource;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_role", nullable = false, length = 20)
    private ShiftRole shiftRole = ShiftRole.STAFF;

    @Column(length = 100)
    private String area;

    @Column(length = 300)
    private String task;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private UserAccount assignedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private UserAccount cancelledBy;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancellationReason;
}
