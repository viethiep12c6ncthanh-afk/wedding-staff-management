package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;
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
        name = "attendance_check_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_check_session_token_hash",
                        columnNames = "token_hash"
                )
        }
)
public class AttendanceCheckSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceCheckAction action;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "radius_meters")
    private Integer radiusMeters;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private UserAccount revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
