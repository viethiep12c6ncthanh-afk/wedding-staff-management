-- Commit 5: normalize manual attendance and basic flat-pay calculation.
-- Run exactly once after V004 and V004_1.

USE wedding_staff_management;

ALTER TABLE attendances
    CHANGE COLUMN status attendance_result VARCHAR(30) NULL,
    ADD COLUMN process_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        AFTER assignment_id,
    ADD COLUMN late_minutes INT UNSIGNED NOT NULL DEFAULT 0
        AFTER check_out_at,
    ADD COLUMN early_leave_minutes INT UNSIGNED NOT NULL DEFAULT 0
        AFTER late_minutes,
    ADD COLUMN recorded_by BIGINT NULL
        AFTER note,
    ADD COLUMN recorded_at DATETIME(6) NULL
        AFTER recorded_by,
    ADD COLUMN confirmed_at DATETIME(6) NULL
        AFTER confirmed_by,
    ADD COLUMN base_pay_snapshot DECIMAL(12,2) NULL
        AFTER confirmed_at,
    ADD COLUMN payable_amount DECIMAL(12,2) NULL
        AFTER base_pay_snapshot;

UPDATE attendances
SET attendance_result = 'EARLY_LEAVE'
WHERE attendance_result = 'LEFT_EARLY';

UPDATE attendances
SET attendance_result = NULL
WHERE attendance_result = 'NOT_RECORDED';

UPDATE attendances attendance
JOIN shift_assignments assignment
    ON assignment.id = attendance.assignment_id
SET attendance.recorded_by = COALESCE(
        attendance.confirmed_by,
        assignment.assigned_by
    ),
    attendance.recorded_at = COALESCE(
        attendance.created_at,
        CURRENT_TIMESTAMP(6)
    ),
    attendance.process_status = CASE
        WHEN attendance.confirmed_by IS NULL THEN 'DRAFT'
        ELSE 'CONFIRMED'
    END,
    attendance.confirmed_at = CASE
        WHEN attendance.confirmed_by IS NULL THEN NULL
        ELSE COALESCE(attendance.updated_at, CURRENT_TIMESTAMP(6))
    END;

UPDATE attendances attendance
JOIN shift_assignments assignment
    ON assignment.id = attendance.assignment_id
JOIN shifts shift
    ON shift.id = assignment.shift_id
SET attendance.late_minutes = CASE
        WHEN attendance.check_in_at IS NULL THEN 0
        ELSE GREATEST(
            TIMESTAMPDIFF(
                MINUTE,
                shift.start_at,
                attendance.check_in_at
            ),
            0
        )
    END,
    attendance.early_leave_minutes = CASE
        WHEN attendance.check_out_at IS NULL THEN 0
        ELSE GREATEST(
            TIMESTAMPDIFF(
                MINUTE,
                attendance.check_out_at,
                shift.end_at
            ),
            0
        )
    END,
    attendance.base_pay_snapshot = CASE
        WHEN attendance.process_status = 'CONFIRMED'
            THEN shift.pay_amount
        ELSE NULL
    END,
    attendance.payable_amount = CASE
        WHEN attendance.process_status <> 'CONFIRMED' THEN NULL
        WHEN attendance.attendance_result = 'ABSENT' THEN 0
        ELSE shift.pay_amount
    END;

UPDATE attendances
SET attendance_result = CASE
        WHEN check_in_at IS NULL AND check_out_at IS NULL THEN 'ABSENT'
        WHEN check_in_at IS NOT NULL AND check_out_at IS NOT NULL
            AND late_minutes > 0 AND early_leave_minutes > 0
            THEN 'LATE_AND_EARLY_LEAVE'
        WHEN check_in_at IS NOT NULL AND check_out_at IS NOT NULL
            AND late_minutes > 0
            THEN 'LATE'
        WHEN check_in_at IS NOT NULL AND check_out_at IS NOT NULL
            AND early_leave_minutes > 0
            THEN 'EARLY_LEAVE'
        WHEN check_in_at IS NOT NULL AND check_out_at IS NOT NULL
            THEN 'PRESENT'
        ELSE NULL
    END
WHERE process_status = 'CONFIRMED'
  AND attendance_result IS NULL;

UPDATE attendances
SET check_in_at = NULL,
    check_out_at = NULL,
    late_minutes = 0,
    early_leave_minutes = 0,
    payable_amount = 0
WHERE attendance_result = 'ABSENT';

UPDATE attendances
SET process_status = 'DRAFT',
    confirmed_by = NULL,
    confirmed_at = NULL,
    base_pay_snapshot = NULL,
    payable_amount = NULL
WHERE process_status = 'CONFIRMED'
  AND attendance_result IS NULL;

ALTER TABLE attendances
    MODIFY COLUMN recorded_by BIGINT NOT NULL,
    MODIFY COLUMN recorded_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),
    ADD CONSTRAINT fk_attendances_recorder
        FOREIGN KEY (recorded_by) REFERENCES users(id),
    ADD CONSTRAINT chk_attendances_process_status CHECK (
        process_status IN ('DRAFT', 'CONFIRMED')
    ),
    ADD CONSTRAINT chk_attendances_result CHECK (
        attendance_result IS NULL OR attendance_result IN (
            'PRESENT', 'LATE', 'EARLY_LEAVE',
            'LATE_AND_EARLY_LEAVE', 'ABSENT'
        )
    ),
    ADD CONSTRAINT chk_attendances_time_order CHECK (
        check_in_at IS NULL OR check_out_at IS NULL
        OR check_in_at < check_out_at
    ),
    ADD CONSTRAINT chk_attendances_absent_time CHECK (
        attendance_result <> 'ABSENT'
        OR (check_in_at IS NULL AND check_out_at IS NULL)
    ),
    ADD CONSTRAINT chk_attendances_confirmation CHECK (
        (process_status = 'DRAFT'
            AND confirmed_by IS NULL
            AND confirmed_at IS NULL
            AND base_pay_snapshot IS NULL
            AND payable_amount IS NULL)
        OR
        (process_status = 'CONFIRMED'
            AND attendance_result IS NOT NULL
            AND confirmed_by IS NOT NULL
            AND confirmed_at IS NOT NULL
            AND base_pay_snapshot IS NOT NULL
            AND payable_amount IS NOT NULL)
    ),
    ADD CONSTRAINT chk_attendances_pay CHECK (
        base_pay_snapshot IS NULL OR base_pay_snapshot >= 0
    ),
    ADD CONSTRAINT chk_attendances_payable CHECK (
        payable_amount IS NULL OR payable_amount >= 0
    );

CREATE INDEX idx_attendances_process_status
    ON attendances(process_status);
CREATE INDEX idx_attendances_result
    ON attendances(attendance_result);
CREATE INDEX idx_attendances_confirmed_at
    ON attendances(confirmed_at);
