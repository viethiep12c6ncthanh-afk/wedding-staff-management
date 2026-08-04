package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.RegistrationStatus;
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
        name = "shift_registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_registration_shift_employee",
                columnNames = {"shift_id", "employee_id"}
        )
)
public class ShiftRegistration extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private UserAccount cancelledBy;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancellationReason;
}
