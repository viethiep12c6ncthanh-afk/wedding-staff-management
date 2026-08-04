-- One-time migration for Commit 4.
-- Run exactly once after V003 has completed successfully.

USE wedding_staff_management;

ALTER TABLE shift_registrations
    MODIFY COLUMN reviewed_at DATETIME(6) NULL,
    ADD COLUMN cancelled_by BIGINT NULL AFTER rejection_reason,
    ADD COLUMN cancelled_at DATETIME(6) NULL AFTER cancelled_by,
    ADD COLUMN cancellation_reason VARCHAR(500) NULL
        AFTER cancelled_at,
    ADD CONSTRAINT fk_registrations_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_registrations_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    );

ALTER TABLE shift_assignments
    ADD COLUMN registration_id BIGINT NULL AFTER employee_id,
    ADD COLUMN assignment_source VARCHAR(20) NULL
        AFTER registration_id,
    ADD COLUMN shift_role VARCHAR(20) NULL AFTER assignment_source,
    ADD COLUMN cancelled_by BIGINT NULL AFTER assigned_by,
    ADD COLUMN cancelled_at DATETIME(6) NULL AFTER cancelled_by,
    ADD COLUMN cancellation_reason VARCHAR(500) NULL
        AFTER cancelled_at;

UPDATE shift_assignments assignment
LEFT JOIN shift_registrations registration
    ON registration.shift_id = assignment.shift_id
   AND registration.employee_id = assignment.employee_id
SET assignment.registration_id = registration.id,
    assignment.assignment_source = CASE
        WHEN registration.id IS NULL THEN 'DIRECT'
        ELSE 'REGISTRATION'
    END,
    assignment.shift_role = CASE
        WHEN UPPER(TRIM(COALESCE(assignment.role_in_shift, '')))
                = 'LEADER'
            THEN 'LEADER'
        ELSE 'STAFF'
    END;

ALTER TABLE shift_assignments
    MODIFY COLUMN assignment_source VARCHAR(20) NOT NULL,
    MODIFY COLUMN shift_role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    DROP COLUMN role_in_shift,
    ADD CONSTRAINT uk_assignment_registration UNIQUE (registration_id),
    ADD CONSTRAINT fk_assignments_registration
        FOREIGN KEY (registration_id) REFERENCES shift_registrations(id),
    ADD CONSTRAINT fk_assignments_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_assignments_source CHECK (
        (assignment_source = 'REGISTRATION' AND registration_id IS NOT NULL)
        OR (assignment_source = 'DIRECT' AND registration_id IS NULL)
    ),
    ADD CONSTRAINT chk_assignments_shift_role CHECK (
        shift_role IN ('LEADER', 'STAFF')
    ),
    ADD CONSTRAINT chk_assignments_status CHECK (
        status IN (
            'ASSIGNED', 'CONFIRMED', 'COMPLETED',
            'ABSENT', 'CANCELLED'
        )
    );

CREATE INDEX idx_assignments_shift_status
    ON shift_assignments(shift_id, status);

CREATE INDEX idx_assignments_source
    ON shift_assignments(assignment_source);
