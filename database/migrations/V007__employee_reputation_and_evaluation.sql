-- DACN Commit 3: employee reputation ledger and evaluation workflow.
-- Run exactly once after the DACS V006 schema is present.
-- Reputation starts at 80/100 when this module is activated.
-- Historical DACS attendance is intentionally not scored retroactively;
-- only attendance confirmed after this module is active creates reputation events.

SET NAMES utf8mb4;

USE wedding_staff_management;

CREATE TABLE employee_reputations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    current_score INT NOT NULL DEFAULT 80,
    completed_shift_count INT NOT NULL DEFAULT 0,
    late_count INT NOT NULL DEFAULT 0,
    early_leave_count INT NOT NULL DEFAULT 0,
    absent_count INT NOT NULL DEFAULT 0,
    evaluation_count INT NOT NULL DEFAULT 0,
    rating_sum INT NOT NULL DEFAULT 0,
    last_event_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_employee_reputation_employee UNIQUE (employee_id),
    CONSTRAINT fk_employee_reputation_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT chk_employee_reputation_score
        CHECK (current_score BETWEEN 0 AND 100),
    CONSTRAINT chk_employee_reputation_counters CHECK (
        completed_shift_count >= 0
        AND late_count >= 0
        AND early_leave_count >= 0
        AND absent_count >= 0
        AND evaluation_count >= 0
        AND rating_sum >= 0
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE employee_evaluations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(500),
    evaluated_by BIGINT NOT NULL,
    evaluated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_employee_evaluation_assignment UNIQUE (assignment_id),
    CONSTRAINT fk_employee_evaluation_assignment
        FOREIGN KEY (assignment_id) REFERENCES shift_assignments(id),
    CONSTRAINT fk_employee_evaluation_user
        FOREIGN KEY (evaluated_by) REFERENCES users(id),
    CONSTRAINT chk_employee_evaluation_rating
        CHECK (rating BETWEEN 1 AND 5)
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE reputation_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT NOT NULL,
    score_before INT NOT NULL,
    score_delta INT NOT NULL,
    score_after INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    actor_user_id BIGINT,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_reputation_event_source
        UNIQUE (source_type, source_id),
    CONSTRAINT fk_reputation_event_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_reputation_event_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_reputation_event_type CHECK (
        event_type IN (
            'BASELINE_INITIALIZED',
            'ATTENDANCE_PRESENT',
            'ATTENDANCE_LATE',
            'ATTENDANCE_EARLY_LEAVE',
            'ATTENDANCE_LATE_AND_EARLY_LEAVE',
            'ATTENDANCE_ABSENT',
            'EVALUATION_RATING'
        )
    ),
    CONSTRAINT chk_reputation_source_type CHECK (
        source_type IN ('BASELINE', 'ATTENDANCE', 'EVALUATION')
    ),
    CONSTRAINT chk_reputation_scores CHECK (
        score_before BETWEEN 0 AND 100
        AND score_after BETWEEN 0 AND 100
        AND score_before + score_delta = score_after
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_employee_reputations_score
    ON employee_reputations(current_score);
CREATE INDEX idx_employee_evaluations_evaluator
    ON employee_evaluations(evaluated_by, evaluated_at);
CREATE INDEX idx_reputation_events_employee_time
    ON reputation_events(employee_id, occurred_at);
CREATE INDEX idx_reputation_events_type
    ON reputation_events(event_type);

-- Existing employees begin from the module activation baseline.
INSERT INTO employee_reputations (
    employee_id,
    current_score,
    completed_shift_count,
    late_count,
    early_leave_count,
    absent_count,
    evaluation_count,
    rating_sum,
    last_event_at
)
SELECT
    employee.id,
    80,
    0,
    0,
    0,
    0,
    0,
    0,
    CURRENT_TIMESTAMP(6)
FROM employees employee;

INSERT INTO reputation_events (
    employee_id,
    event_type,
    source_type,
    source_id,
    score_before,
    score_delta,
    score_after,
    reason,
    actor_user_id,
    occurred_at
)
SELECT
    employee.id,
    'BASELINE_INITIALIZED',
    'BASELINE',
    employee.id,
    80,
    0,
    80,
    'Khởi tạo điểm uy tín ở mức trung tính 80/100',
    NULL,
    CURRENT_TIMESTAMP(6)
FROM employees employee;
