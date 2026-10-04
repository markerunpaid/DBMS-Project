-- Sample data for quick_commerce (run after schema.sql)
-- Every login below uses the password: QuickCart@2026
-- (not "password123": Chrome flags that as a leaked password and pops a
--  blocking "Change your password" dialog right after login)
--   customer : customer@qc.com
--   admin    : admin@qc.com   (manages Lanka + Sigra stores)
--   admin    : admin2@qc.com  (manages Sarnath store)
--   partner  : partner1@qc.com, partner2@qc.com, partner3@qc.com
USE quick_commerce;

SET @pw = '$2a$10$rvvhvPq3fC7x5lDQUMehTOraZ9ePdNskYe1FZHjYPjbQyCcAKywpG';

INSERT INTO pincode (pin_code, city, state) VALUES
('221001', 'Varanasi', 'Uttar Pradesh'),
('221002', 'Varanasi', 'Uttar Pradesh'),
('221005', 'Varanasi', 'Uttar Pradesh'),
('221007', 'Varanasi', 'Uttar Pradesh'),
('221010', 'Varanasi', 'Uttar Pradesh');

INSERT INTO address (address_id, house_no, street, pin_code, latitude, longitude) VALUES
(1, 'Shop 4',   'Lanka Main Road',        '221005', 25.275000, 82.999000),  -- Lanka store
(2, 'Plot 12',  'Sigra Chauraha',         '221010', 25.317600, 82.987000),  -- Sigra store
(3, 'Unit 7',   'Ashok Marg, Sarnath',    '221007', 25.381000, 83.024000),  -- Sarnath store
(4, 'Room 214', 'IIT BHU Hostel Road',    '221005', 25.262000, 82.989000),  -- customer home
(5, '18/42',    'Dashashwamedh, Godowlia','221001', 25.309500, 83.007500);  -- customer work

INSERT INTO category (category_id, name) VALUES
(1, 'Fruits & Vegetables'),
(2, 'Dairy & Bread'),
(3, 'Snacks'),
(4, 'Beverages'),
(5, 'Personal Care');

INSERT INTO product (product_id, name, price, expiry, unit, category_id) VALUES
(1,  'Banana',              48.00, '2026-10-05', '6 pcs',     1),
(2,  'Tomato',              32.00, '2026-10-06', '500 g',     1),
(3,  'Onion',               40.00, '2026-11-15', '1 kg',      1),
(4,  'Apple (Shimla)',     160.00, '2026-10-20', '1 kg',      1),
(5,  'Toned Milk',          28.00, '2026-10-01', '500 ml',    2),
(6,  'Brown Bread',         45.00, '2026-10-03', '400 g',     2),
(7,  'Paneer',              95.00, '2026-10-04', '200 g',     2),
(8,  'Curd',                35.00, '2026-10-03', '400 g',     2),
(9,  'Potato Chips',        20.00, '2027-02-01', '52 g',      3),
(10, 'Salted Peanuts',      60.00, '2027-01-15', '200 g',     3),
(11, 'Chocolate Cookies',   40.00, '2027-03-01', '150 g',     3),
(12, 'Cola',                40.00, '2027-04-01', '750 ml',    4),
(13, 'Orange Juice',       110.00, '2026-12-01', '1 L',       4),
(14, 'Green Tea',          150.00, '2027-08-01', '25 bags',   4),
(15, 'Toothpaste',          95.00, '2028-01-01', '150 g',     5),
(16, 'Hand Wash',           99.00, '2028-01-01', '200 ml',    5);

-- dark stores first (manager set after employees exist -- circular FK)
INSERT INTO dark_store (dark_store_id, name, address_id, manager_employee_id) VALUES
(1, 'Lanka Dark Store',   1, NULL),
(2, 'Sigra Dark Store',   2, NULL),
(3, 'Sarnath Dark Store', 3, NULL);

INSERT INTO employee (employee_id, first_name, middle_name, last_name, phone, email, password_hash, dark_store_id) VALUES
(1, 'Ravi',  NULL,    'Kumar',  '9000000001', 'admin@qc.com',  @pw,  1),
(2, 'Neha',  NULL,    'Singh',  '9000000002', 'admin2@qc.com', @pw,  3),
(3, 'Suresh','Prasad','Yadav',  '9000000003', NULL,            NULL, 1);   -- floor staff, no login

UPDATE dark_store SET manager_employee_id = 1 WHERE dark_store_id IN (1, 2);
UPDATE dark_store SET manager_employee_id = 2 WHERE dark_store_id = 3;

-- stores 1 and 2 run 24x7 so the app can be tested any time; Sarnath has day hours
INSERT INTO operating_hours (dark_store_id, day_of_week, opens_at, closes_at)
SELECT s.id, d.day, s.opens, s.closes
FROM (SELECT 1 AS id, '00:00:00' AS opens, '23:59:59' AS closes
      UNION ALL SELECT 2, '00:00:00', '23:59:59'
      UNION ALL SELECT 3, '07:00:00', '23:00:00') s
CROSS JOIN (SELECT 'MON' AS day UNION ALL SELECT 'TUE' UNION ALL SELECT 'WED'
            UNION ALL SELECT 'THU' UNION ALL SELECT 'FRI' UNION ALL SELECT 'SAT'
            UNION ALL SELECT 'SUN') d;

INSERT INTO dark_store_category (dark_store_id, category_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(2, 1), (2, 2), (2, 3), (2, 4),
(3, 1), (3, 2), (3, 5);

INSERT INTO inventory (dark_store_id, product_id, quantity) VALUES
(1, 1, 40), (1, 2, 60), (1, 3, 50), (1, 4, 20), (1, 5, 80), (1, 6, 25), (1, 7, 15), (1, 8, 30),
(1, 9, 100), (1, 10, 40), (1, 11, 35), (1, 12, 50), (1, 13, 12), (1, 14, 10), (1, 15, 20), (1, 16, 18),
(2, 1, 30), (2, 2, 25), (2, 3, 45), (2, 5, 60), (2, 6, 20), (2, 7, 3), (2, 9, 70), (2, 10, 25),
(2, 11, 20), (2, 12, 40), (2, 13, 0),
(3, 1, 15), (3, 3, 20), (3, 4, 10), (3, 5, 30), (3, 8, 12), (3, 15, 8), (3, 16, 6);

INSERT INTO delivery_partner (partner_id, first_name, middle_name, last_name, phone, email, password_hash, vehicle_number, status, dark_store_id) VALUES
(1, 'Amit',   NULL, 'Verma',   '9100000001', 'partner1@qc.com', @pw, 'UP65 AB 1234', 'AVAILABLE', 1),
(2, 'Rahul',  NULL, 'Gupta',   '9100000002', 'partner2@qc.com', @pw, NULL,           'AVAILABLE', 1),
(3, 'Vikram', NULL, 'Chauhan', '9100000003', 'partner3@qc.com', @pw, 'UP65 CD 5678', 'AVAILABLE', 2);

INSERT INTO customer (customer_id, first_name, middle_name, last_name, phone, email, password_hash) VALUES
(1, 'Aarav', NULL, 'Sharma', '9876543210', 'customer@qc.com', @pw);

INSERT INTO customer_address (customer_id, address_id, label, is_default) VALUES
(1, 4, 'Home', TRUE),
(1, 5, 'Work', FALSE);

INSERT INTO coupon (coupon_id, code, discount_type, value, valid_from, valid_to) VALUES
(1, 'WELCOME50', 'FLAT',       50.00, '2026-01-01 00:00:00', '2027-12-31 23:59:59'),
(2, 'SAVE10',    'PERCENTAGE', 10.00, '2026-01-01 00:00:00', '2027-12-31 23:59:59'),
(3, 'OLD20',     'PERCENTAGE', 20.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59'),  -- expired
(4, 'FIRST100',  'FLAT',      100.00, '2026-01-01 00:00:00', '2027-12-31 23:59:59');  -- already redeemed

INSERT INTO customer_coupon (customer_id, coupon_id, redeemed_at) VALUES
(1, 1, NULL),
(1, 2, NULL),
(1, 3, NULL),
(1, 4, '2026-09-20 18:40:00');

-- one past, delivered order so order history isn't empty
INSERT INTO orders (order_id, customer_id, dark_store_id, delivery_partner_id, coupon_id, delivery_address_id, amount, status) VALUES
(1, 1, 1, 1, 4, 4, 196.00, 'DELIVERED');

INSERT INTO order_product (order_id, product_id, quantity, price_at_order) VALUES
(1, 4, 1, 160.00),
(1, 7, 1,  95.00),
(1, 12, 1, 41.00);   -- cola was priced 41 back then; price_at_order freezes it

INSERT INTO order_timer (order_id, placed_at, expected_delivery_at, out_for_delivery_at, received_at, cancelled_at) VALUES
(1, '2026-09-20 18:40:00', '2026-09-20 18:52:00', '2026-09-20 18:44:00', '2026-09-20 18:51:00', NULL);

INSERT INTO payment_record (order_id, amount, mode, status, timestamp) VALUES
(1, 196.00, 'UPI', 'SUCCESS', '2026-09-20 18:40:00');
