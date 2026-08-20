-- DACN Commit 4: cancellation and replacement staffing workflow.
-- Run exactly once after V007.
-- Employee self-cancellation rules from DACS remain unchanged. This migration
-- introduces a separate request/review/invitation workflow before a shift starts.

SET NAMES utf8mb4;

USE wedding_staff_management;

ALTER TABLE shift_assignments
    DROP CHECK chk_assignments_source;

ALTER TABLE shift_assignments
    ADD CONSTRAINT chk_assignments_source CHECK (
        (assignment_source = 'REGISTRATION' AND registration_id IS NOT NULL)
        OR (
            assignment_source IN ('DIRECT', 'REPLACEMENT')
            AND registration_id IS NULL
        )
    );

CREATE TABLE replacement_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_assignment_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    review_note VARCHAR(500) NULL,
    replacement_assignment_id BIGINT NULL,
    filled_at DATETIME(6) NULL,
    closed_at DATETIME(6) NULL,
    closed_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_replacement_request_original_assignment
        FOREIGN KEY (original_assignment_id) REFERENCES shift_assignments(id),
    CONSTRAINT fk_replacement_request_requested_by
        FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT fk_replacement_request_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_replacement_request_replacement_assignment
        FOREIGN KEY (replacement_assignment_id) REFERENCES shift_assignments(id),
    CONSTRAINT uk_replacement_request_replacement_assignment
        UNIQUE (replacement_assignment_id),
    CONSTRAINT chk_replacement_request_status CHECK (
        status IN ('PENDING', 'OPEN', 'FILLED', 'REJECTED', 'CANCELLED')
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE replacement_invitations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invited_by BIGINT NOT NULL,
    invited_at DATETIME(6) NOT NULL,
    responded_at DATETIME(6) NULL,
    response_note VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_replacement_invitation_request
        FOREIGN KEY (request_id) REFERENCES replacement_requests(id),
    CONSTRAINT fk_replacement_invitation_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_replacement_invitation_invited_by
        FOREIGN KEY (invited_by) REFERENCES users(id),
    CONSTRAINT uk_replacement_invitation_request_employee
        UNIQUE (request_id, employee_id),
    CONSTRAINT chk_replacement_invitation_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_replacement_requests_status
    ON replacement_requests(status, created_at);
CREATE INDEX idx_replacement_requests_original_assignment
    ON replacement_requests(original_assignment_id, status);
CREATE INDEX idx_replacement_invitations_employee_status
    ON replacement_invitations(employee_id, status, invited_at);
CREATE INDEX idx_replacement_invitations_request_status
    ON replacement_invitations(request_id, status);
