package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.ShiftStatus;
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
@Table(name = "shifts")
public class WorkShift extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private Integer requiredStaff;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal payAmount;

    @Builder.Default
    @Column(nullable = false)
    private boolean registrationOpen = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShiftStatus status;

    @Column(length = 500)
    private String note;
}
