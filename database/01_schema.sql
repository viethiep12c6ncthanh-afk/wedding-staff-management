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
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) UNIQUE,
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    experience_level VARCHAR(50),
    notes VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE venues (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(300) NOT NULL,
    contact_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    venue_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL,
    description VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues(id),
    CONSTRAINT chk_event_time CHECK (end_at > start_at)
);

CREATE TABLE shifts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    required_staff INT NOT NULL,
    pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    registration_open BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    note VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_shifts_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT chk_shift_time CHECK (end_at > start_at),
    CONSTRAINT chk_required_staff CHECK (required_staff > 0)
);

CREATE TABLE shift_registrations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    rejection_reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_registration_shift_employee UNIQUE (shift_id, employee_id),
    CONSTRAINT fk_registrations_shift FOREIGN KEY (shift_id) REFERENCES shifts(id),
    CONSTRAINT fk_registrations_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_registrations_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE shift_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    role_in_shift VARCHAR(80),
    area VARCHAR(100),
    task VARCHAR(300),
    status VARCHAR(30) NOT NULL,
    assigned_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_assignment_shift_employee UNIQUE (shift_id, employee_id),
    CONSTRAINT fk_assignments_shift FOREIGN KEY (shift_id) REFERENCES shifts(id),
    CONSTRAINT fk_assignments_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_assignments_assigner FOREIGN KEY (assigned_by) REFERENCES users(id)
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
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendances_assignment FOREIGN KEY (assignment_id) REFERENCES shift_assignments(id),
    CONSTRAINT fk_attendances_confirmer FOREIGN KEY (confirmed_by) REFERENCES users(id)
);

CREATE INDEX idx_events_venue ON events(venue_id);
CREATE INDEX idx_shifts_event_time ON shifts(event_id, start_at, end_at);
CREATE INDEX idx_registrations_employee_status ON shift_registrations(employee_id, status);
CREATE INDEX idx_assignments_employee_status ON shift_assignments(employee_id, status);
