-- DACN Commit 7: QR/OTP self-attendance sessions and immutable verification events.
-- Run exactly once after V009.
-- Existing manual attendance and DRAFT -> CONFIRMED workflow remain unchanged.

SET NAMES utf8mb4;

USE wedding_staff_management;

CREATE TABLE attendance_check_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    otp_hash VARCHAR(100) NOT NULL,
    valid_from DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    radius_meters INT UNSIGNED NULL,
    created_by BIGINT NOT NULL,
    revoked_by BIGINT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_attendance_check_session_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_attendance_check_session_shift
        FOREIGN KEY (shift_id) REFERENCES shifts(id),
    CONSTRAINT fk_attendance_check_session_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_attendance_check_session_revoked_by
        FOREIGN KEY (revoked_by) REFERENCES users(id),

    CONSTRAINT chk_attendance_check_session_action CHECK (
        action IN ('CHECK_IN', 'CHECK_OUT')
    ),
    CONSTRAINT chk_attendance_check_session_time CHECK (
        valid_from < expires_at
    ),
    CONSTRAINT chk_attendance_check_session_location_pair CHECK (
        (latitude IS NULL AND longitude IS NULL AND radius_meters IS NULL)
        OR
        (latitude IS NOT NULL AND longitude IS NOT NULL
            AND radius_meters IS NOT NULL)
    ),
    CONSTRAINT chk_attendance_check_session_latitude CHECK (
        latitude IS NULL OR latitude BETWEEN -90 AND 90
    ),
    CONSTRAINT chk_attendance_check_session_longitude CHECK (
        longitude IS NULL OR longitude BETWEEN -180 AND 180
    ),
    CONSTRAINT chk_attendance_check_session_radius CHECK (
        radius_meters IS NULL OR radius_meters BETWEEN 20 AND 1000
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_attendance_check_session_shift_action
    ON attendance_check_sessions(shift_id, action, expires_at);

CREATE INDEX idx_attendance_check_session_active
    ON attendance_check_sessions(shift_id, action, revoked_at, expires_at);

CREATE TABLE attendance_check_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attendance_id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    method VARCHAR(20) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    distance_meters INT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_attendance_check_event_action
        UNIQUE (attendance_id, action),

    CONSTRAINT fk_attendance_check_event_attendance
        FOREIGN KEY (attendance_id) REFERENCES attendances(id),
    CONSTRAINT fk_attendance_check_event_assignment
        FOREIGN KEY (assignment_id) REFERENCES shift_assignments(id),
    CONSTRAINT fk_attendance_check_event_session
        FOREIGN KEY (session_id) REFERENCES attendance_check_sessions(id),

    CONSTRAINT chk_attendance_check_event_action CHECK (
        action IN ('CHECK_IN', 'CHECK_OUT')
    ),
    CONSTRAINT chk_attendance_check_event_method CHECK (
        method IN ('QR', 'OTP')
    ),
    CONSTRAINT chk_attendance_check_event_location_pair CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR
        (latitude IS NOT NULL AND longitude IS NOT NULL)
    ),
    CONSTRAINT chk_attendance_check_event_latitude CHECK (
        latitude IS NULL OR latitude BETWEEN -90 AND 90
    ),
    CONSTRAINT chk_attendance_check_event_longitude CHECK (
        longitude IS NULL OR longitude BETWEEN -180 AND 180
    ),
    CONSTRAINT chk_attendance_check_event_distance CHECK (
        distance_meters IS NULL OR distance_meters >= 0
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_attendance_check_event_assignment_time
    ON attendance_check_events(assignment_id, occurred_at);

CREATE INDEX idx_attendance_check_event_session_time
    ON attendance_check_events(session_id, occurred_at);
