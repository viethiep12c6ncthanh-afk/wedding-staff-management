-- Commit 6: employee profile and account management.
-- Run exactly once after V005.

USE wedding_staff_management;

ALTER TABLE users
    DROP CHECK chk_users_account_status;

ALTER TABLE users
    ADD CONSTRAINT chk_users_account_status
        CHECK (account_status IN ('ACTIVE', 'INACTIVE', 'LOCKED'));

ALTER TABLE employees
    DROP CHECK chk_employees_employment_status;

UPDATE employees
SET employment_status = 'ON_LEAVE'
WHERE employment_status = 'SUSPENDED';

ALTER TABLE employees
    ADD CONSTRAINT chk_employees_employment_status
        CHECK (employment_status IN ('ACTIVE', 'ON_LEAVE', 'INACTIVE'));

