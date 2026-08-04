CREATE DATABASE IF NOT EXISTS wedding_staff_management
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE wedding_staff_management;

CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at DATETIME(6),
    created_by BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_users_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_users_account_status
        CHECK (account_status IN ('ACTIVE', 'LOCKED'))
);

CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    date_of_birth DATE,
    address VARCHAR(255),
    experience_level VARCHAR(50),
    note VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_employees_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_employees_employment_status
        CHECK (employment_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE TABLE venues (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(300) NOT NULL,
    contact_name VARCHAR(120),
    contact_phone VARCHAR(20),
    venue_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    note VARCHAR(500),
    created_by BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_venues_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_venues_status
        CHECK (venue_status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    venue_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    event_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    description VARCHAR(1000),
    created_by BIGINT,
    cancelled_by BIGINT,
    cancelled_at DATETIME(6),
    cancellation_reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_events_venue
        FOREIGN KEY (venue_id) REFERENCES venues(id),
    CONSTRAINT fk_events_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_events_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_event_time CHECK (end_at > start_at),
    CONSTRAINT chk_events_status CHECK (
        event_status IN (
            'DRAFT', 'CONFIRMED', 'IN_PROGRESS',
            'COMPLETED', 'CANCELLED'
        )
    )
);

CREATE TABLE shifts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    required_staff INT NOT NULL,
    pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    registration_deadline DATETIME(6),
    shift_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    description VARCHAR(500),
    created_by BIGINT,
    cancelled_by BIGINT,
    cancelled_at DATETIME(6),
    cancellation_reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_shifts_event
        FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_shifts_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_shifts_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_shift_time CHECK (end_at > start_at),
    CONSTRAINT chk_required_staff CHECK (required_staff > 0),
    CONSTRAINT chk_pay_amount CHECK (pay_amount >= 0),
    CONSTRAINT chk_shift_registration_deadline CHECK (
        registration_deadline IS NULL
        OR registration_deadline <= start_at
    ),
    CONSTRAINT chk_shifts_status CHECK (
        shift_status IN (
            'DRAFT', 'OPEN', 'CLOSED',
            'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
        )
    )
);

CREATE TABLE shift_registrations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewed_at DATETIME(6),
    rejection_reason VARCHAR(500),
    cancelled_by BIGINT,
    cancelled_at DATETIME(6),
    cancellation_reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_registration_shift_employee
        UNIQUE (shift_id, employee_id),
    CONSTRAINT fk_registrations_shift
        FOREIGN KEY (shift_id) REFERENCES shifts(id),
    CONSTRAINT fk_registrations_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_registrations_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_registrations_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_registrations_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    )
);

CREATE TABLE shift_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    registration_id BIGINT UNIQUE,
    assignment_source VARCHAR(20) NOT NULL,
    shift_role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    area VARCHAR(100),
    task VARCHAR(300),
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    assigned_by BIGINT NOT NULL,
    cancelled_by BIGINT,
    cancelled_at DATETIME(6),
    cancellation_reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_assignments_shift
        FOREIGN KEY (shift_id) REFERENCES shifts(id),
    CONSTRAINT fk_assignments_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_assignments_registration
        FOREIGN KEY (registration_id) REFERENCES shift_registrations(id),
    CONSTRAINT fk_assignments_assigner
        FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT fk_assignments_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_assignments_source CHECK (
        (assignment_source = 'REGISTRATION' AND registration_id IS NOT NULL)
        OR (assignment_source = 'DIRECT' AND registration_id IS NULL)
    ),
    CONSTRAINT chk_assignments_shift_role CHECK (
        shift_role IN ('LEADER', 'STAFF')
    ),
    CONSTRAINT chk_assignments_status CHECK (
        status IN (
            'ASSIGNED', 'CONFIRMED', 'COMPLETED',
            'ABSENT', 'CANCELLED'
        )
    )
);

CREATE TABLE attendances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL UNIQUE,
    check_in_at DATETIME,
    check_out_at DATETIME,
    status VARCHAR(30) NOT NULL,
    note VARCHAR(500),
    confirmed_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendances_assignment
        FOREIGN KEY (assignment_id) REFERENCES shift_assignments(id),
    CONSTRAINT fk_attendances_confirmer
        FOREIGN KEY (confirmed_by) REFERENCES users(id)
);

CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_account_status ON users(account_status);
CREATE INDEX idx_employees_employment_status
    ON employees(employment_status);
CREATE INDEX idx_venues_status ON venues(venue_status);
CREATE INDEX idx_events_venue_start ON events(venue_id, start_at);
CREATE INDEX idx_events_status ON events(event_status);
CREATE INDEX idx_shifts_event_time
    ON shifts(event_id, start_at, end_at);
CREATE INDEX idx_shifts_status_start
    ON shifts(shift_status, start_at);
CREATE INDEX idx_registrations_employee_status
    ON shift_registrations(employee_id, status);
CREATE INDEX idx_assignments_employee_status
    ON shift_assignments(employee_id, status);
CREATE INDEX idx_assignments_shift_employee
    ON shift_assignments(shift_id, employee_id);
CREATE INDEX idx_assignments_shift_status
    ON shift_assignments(shift_id, status);
CREATE INDEX idx_assignments_source
    ON shift_assignments(assignment_source);
