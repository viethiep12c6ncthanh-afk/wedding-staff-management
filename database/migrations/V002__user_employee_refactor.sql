-- One-time migration from the starter schema to Commit 2.
-- Run exactly once on the existing wedding_staff_management database.

USE wedding_staff_management;

-- Preserve the existing BCrypt values while renaming the password column.
ALTER TABLE users
    CHANGE COLUMN password password_hash VARCHAR(255) NOT NULL;

-- Convert the old boolean enabled flag into the explicit account status.
ALTER TABLE users
    ADD COLUMN account_status VARCHAR(20) NULL AFTER phone,
    ADD COLUMN must_change_password BOOLEAN NULL DEFAULT TRUE AFTER account_status,
    ADD COLUMN last_login_at DATETIME(6) NULL AFTER must_change_password,
    ADD COLUMN created_by BIGINT NULL AFTER last_login_at;

UPDATE users
SET account_status =
    CASE
        WHEN enabled = TRUE THEN 'ACTIVE'
        ELSE 'LOCKED'
    END
WHERE account_status IS NULL;

-- Existing accounts have already been used, so do not force a password change.
UPDATE users
SET must_change_password = FALSE
WHERE must_change_password IS NULL;

ALTER TABLE users
    MODIFY COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    MODIFY COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    DROP COLUMN enabled,
    ADD CONSTRAINT uk_users_phone UNIQUE (phone),
    ADD CONSTRAINT fk_users_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_users_account_status
        CHECK (account_status IN ('ACTIVE', 'LOCKED'));

CREATE INDEX idx_users_role_id
    ON users(role_id);

CREATE INDEX idx_users_account_status
    ON users(account_status);

-- Rename employee fields and add the profile fields required by the baseline.
ALTER TABLE employees
    CHANGE COLUMN status employment_status VARCHAR(20) NOT NULL,
    CHANGE COLUMN notes note VARCHAR(500) NULL,
    ADD COLUMN date_of_birth DATE NULL AFTER employment_status,
    ADD COLUMN address VARCHAR(255) NULL AFTER date_of_birth,
    ADD CONSTRAINT chk_employees_employment_status
        CHECK (employment_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));

CREATE INDEX idx_employees_employment_status
    ON employees(employment_status);
