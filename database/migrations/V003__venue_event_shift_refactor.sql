-- One-time migration for Commit 3.
-- Run exactly once after V002 has completed successfully.

USE wedding_staff_management;

ALTER TABLE venues
    CHANGE COLUMN status venue_status VARCHAR(20) NOT NULL,
    ADD COLUMN contact_name VARCHAR(120) NULL AFTER address,
    ADD COLUMN note VARCHAR(500) NULL AFTER venue_status,
    ADD COLUMN created_by BIGINT NULL AFTER note,
    ADD CONSTRAINT fk_venues_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_venues_status
        CHECK (venue_status IN ('ACTIVE', 'INACTIVE'));

CREATE INDEX idx_venues_status
    ON venues(venue_status);

ALTER TABLE events
    CHANGE COLUMN status event_status VARCHAR(20) NOT NULL,
    ADD COLUMN created_by BIGINT NULL AFTER description,
    ADD COLUMN cancelled_by BIGINT NULL AFTER created_by,
    ADD COLUMN cancelled_at DATETIME(6) NULL AFTER cancelled_by,
    ADD COLUMN cancellation_reason VARCHAR(500) NULL
        AFTER cancelled_at,
    ADD CONSTRAINT fk_events_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_events_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_events_status CHECK (
        event_status IN (
            'DRAFT', 'CONFIRMED', 'IN_PROGRESS',
            'COMPLETED', 'CANCELLED'
        )
    );

CREATE INDEX idx_events_status
    ON events(event_status);

ALTER TABLE shifts
    CHANGE COLUMN status shift_status VARCHAR(20) NOT NULL,
    CHANGE COLUMN note description VARCHAR(500) NULL,
    DROP COLUMN registration_open,
    ADD COLUMN registration_deadline DATETIME(6) NULL
        AFTER pay_amount,
    ADD COLUMN created_by BIGINT NULL AFTER description,
    ADD COLUMN cancelled_by BIGINT NULL AFTER created_by,
    ADD COLUMN cancelled_at DATETIME(6) NULL AFTER cancelled_by,
    ADD COLUMN cancellation_reason VARCHAR(500) NULL
        AFTER cancelled_at,
    ADD CONSTRAINT fk_shifts_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_shifts_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_pay_amount CHECK (pay_amount >= 0),
    ADD CONSTRAINT chk_shift_registration_deadline CHECK (
        registration_deadline IS NULL
        OR registration_deadline <= start_at
    ),
    ADD CONSTRAINT chk_shifts_status CHECK (
        shift_status IN (
            'DRAFT', 'OPEN', 'CLOSED',
            'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
        )
    );

CREATE INDEX idx_shifts_status_start
    ON shifts(shift_status, start_at);
