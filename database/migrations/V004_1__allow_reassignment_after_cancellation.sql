-- Commit 4 hotfix.
-- Allows an employee to be assigned again to the same shift after the
-- previous assignment has been cancelled, while preserving assignment history.
-- Run exactly once after V004.

USE wedding_staff_management;

ALTER TABLE shift_assignments
    DROP INDEX uk_assignment_shift_employee;

CREATE INDEX idx_assignments_shift_employee
    ON shift_assignments(shift_id, employee_id);
