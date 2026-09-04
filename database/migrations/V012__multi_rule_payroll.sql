-- DACN Commit 10: multi-rule payroll snapshots.
-- Run exactly once after V011.
-- Historical CONFIRMED attendances keep base_pay_snapshot/payable_amount unchanged.

SET NAMES utf8mb4;

USE wedding_staff_management;

ALTER TABLE attendances
    ADD COLUMN payroll_policy_version VARCHAR(40) NULL
        AFTER payable_amount,
    ADD COLUMN leader_allowance_snapshot DECIMAL(12,2) NULL
        AFTER payroll_policy_version,
    ADD COLUMN late_deduction_snapshot DECIMAL(12,2) NULL
        AFTER leader_allowance_snapshot,
    ADD COLUMN early_leave_deduction_snapshot DECIMAL(12,2) NULL
        AFTER late_deduction_snapshot,
    ADD COLUMN overtime_minutes_snapshot INT UNSIGNED NULL
        AFTER early_leave_deduction_snapshot,
    ADD COLUMN overtime_pay_snapshot DECIMAL(12,2) NULL
        AFTER overtime_minutes_snapshot;

-- Existing confirmed records were settled under the legacy flat-pay policy.
-- Do not recompute their historical payable_amount.
UPDATE attendances
SET payroll_policy_version = 'DACS_FLAT_V1',
    leader_allowance_snapshot = 0.00,
    late_deduction_snapshot = 0.00,
    early_leave_deduction_snapshot = 0.00,
    overtime_minutes_snapshot = 0,
    overtime_pay_snapshot = 0.00
WHERE process_status = 'CONFIRMED';

ALTER TABLE attendances
    DROP CHECK chk_attendances_confirmation;

ALTER TABLE attendances
    ADD CONSTRAINT chk_attendances_confirmation CHECK (
        (process_status = 'DRAFT'
            AND confirmed_by IS NULL
            AND confirmed_at IS NULL
            AND base_pay_snapshot IS NULL
            AND payable_amount IS NULL
            AND payroll_policy_version IS NULL
            AND leader_allowance_snapshot IS NULL
            AND late_deduction_snapshot IS NULL
            AND early_leave_deduction_snapshot IS NULL
            AND overtime_minutes_snapshot IS NULL
            AND overtime_pay_snapshot IS NULL)
        OR
        (process_status = 'CONFIRMED'
            AND attendance_result IS NOT NULL
            AND confirmed_by IS NOT NULL
            AND confirmed_at IS NOT NULL
            AND base_pay_snapshot IS NOT NULL
            AND payable_amount IS NOT NULL
            AND payroll_policy_version IS NOT NULL
            AND leader_allowance_snapshot IS NOT NULL
            AND late_deduction_snapshot IS NOT NULL
            AND early_leave_deduction_snapshot IS NOT NULL
            AND overtime_minutes_snapshot IS NOT NULL
            AND overtime_pay_snapshot IS NOT NULL)
    ),
    ADD CONSTRAINT chk_attendances_leader_allowance CHECK (
        leader_allowance_snapshot IS NULL
        OR leader_allowance_snapshot >= 0
    ),
    ADD CONSTRAINT chk_attendances_late_deduction CHECK (
        late_deduction_snapshot IS NULL
        OR late_deduction_snapshot >= 0
    ),
    ADD CONSTRAINT chk_attendances_early_leave_deduction CHECK (
        early_leave_deduction_snapshot IS NULL
        OR early_leave_deduction_snapshot >= 0
    ),
    ADD CONSTRAINT chk_attendances_overtime_minutes CHECK (
        overtime_minutes_snapshot IS NULL
        OR overtime_minutes_snapshot >= 0
    ),
    ADD CONSTRAINT chk_attendances_overtime_pay CHECK (
        overtime_pay_snapshot IS NULL
        OR overtime_pay_snapshot >= 0
    );

CREATE INDEX idx_attendances_payroll_policy
    ON attendances(payroll_policy_version);
