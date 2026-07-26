USE wedding_staff_management;

INSERT IGNORE INTO roles(name) VALUES ('ADMIN'), ('COORDINATOR'), ('EMPLOYEE');

INSERT INTO venues(name, address, contact_phone, status)
SELECT 'Trung tâm tiệc cưới mẫu', 'TP. Hồ Chí Minh', '0900000000', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM venues WHERE name = 'Trung tâm tiệc cưới mẫu');
