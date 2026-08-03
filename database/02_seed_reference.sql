USE wedding_staff_management;

INSERT IGNORE INTO roles(name)
VALUES ('ADMIN'), ('COORDINATOR'), ('EMPLOYEE');

INSERT INTO venues(
    name,
    address,
    contact_name,
    contact_phone,
    venue_status,
    note
)
SELECT
    'Trung tâm tiệc cưới mẫu',
    'TP. Hồ Chí Minh',
    'Người liên hệ mẫu',
    '0900000000',
    'ACTIVE',
    'Dữ liệu tham khảo'
WHERE NOT EXISTS (
    SELECT 1
    FROM venues
    WHERE name = 'Trung tâm tiệc cưới mẫu'
);
