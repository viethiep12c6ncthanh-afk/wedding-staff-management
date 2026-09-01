-- DACN Commit 9: structured shift areas, tables, and assignment placement.
-- Run exactly once after V010.
-- Legacy free-text shift_assignments.area values are migrated into shift_areas.

SET NAMES utf8mb4;

USE wedding_staff_management;

CREATE TABLE shift_areas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shift_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    required_staff INT NULL,
    description VARCHAR(500) NULL,
    area_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_shift_area_name
        UNIQUE (shift_id, name),

    CONSTRAINT fk_shift_area_shift
        FOREIGN KEY (shift_id) REFERENCES shifts(id),
    CONSTRAINT fk_shift_area_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_shift_area_required_staff CHECK (
        required_staff IS NULL OR required_staff >= 1
    ),
    CONSTRAINT chk_shift_area_status CHECK (
        area_status IN ('ACTIVE', 'INACTIVE')
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_shift_area_shift_status
    ON shift_areas(shift_id, area_status);

CREATE TABLE shift_tables (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    area_id BIGINT NOT NULL,
    table_code VARCHAR(30) NOT NULL,
    display_name VARCHAR(100) NULL,
    note VARCHAR(300) NULL,
    table_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uk_shift_table_code
        UNIQUE (area_id, table_code),

    CONSTRAINT fk_shift_table_area
        FOREIGN KEY (area_id) REFERENCES shift_areas(id),
    CONSTRAINT fk_shift_table_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_shift_table_status CHECK (
        table_status IN ('ACTIVE', 'INACTIVE')
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_shift_table_area_status
    ON shift_tables(area_id, table_status);

ALTER TABLE shift_assignments
    ADD COLUMN shift_area_id BIGINT NULL AFTER shift_role;

INSERT INTO shift_areas (
    shift_id,
    name,
    required_staff,
    description,
    area_status,
    created_by,
    created_at,
    updated_at
)
SELECT
    assignment.shift_id,
    MIN(TRIM(assignment.area)) AS area_name,
    NULL,
    'Migrated from legacy assignment area',
    'ACTIVE',
    NULL,
    MIN(assignment.created_at),
    MAX(assignment.updated_at)
FROM shift_assignments assignment
WHERE assignment.area IS NOT NULL
  AND TRIM(assignment.area) <> ''
GROUP BY
    assignment.shift_id,
    (LOWER(TRIM(assignment.area)) COLLATE utf8mb4_unicode_ci);

UPDATE shift_assignments assignment
JOIN shift_areas area
  ON area.shift_id = assignment.shift_id
 AND (LOWER(area.name) COLLATE utf8mb4_unicode_ci)
     = (LOWER(TRIM(assignment.area)) COLLATE utf8mb4_unicode_ci)
SET assignment.shift_area_id = area.id
WHERE assignment.area IS NOT NULL
  AND TRIM(assignment.area) <> '';

ALTER TABLE shift_assignments
    ADD CONSTRAINT fk_assignment_shift_area
        FOREIGN KEY (shift_area_id) REFERENCES shift_areas(id),
    DROP COLUMN area;

CREATE INDEX idx_assignments_shift_area_status
    ON shift_assignments(shift_area_id, status);

CREATE TABLE shift_assignment_tables (
    assignment_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,

    PRIMARY KEY (assignment_id, table_id),

    CONSTRAINT fk_assignment_table_assignment
        FOREIGN KEY (assignment_id) REFERENCES shift_assignments(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_assignment_table_table
        FOREIGN KEY (table_id) REFERENCES shift_tables(id)
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_assignment_table_table
    ON shift_assignment_tables(table_id);
