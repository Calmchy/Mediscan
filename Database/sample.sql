-- mediscan pos - sample data (10 rows per table)
-- run this AFTER db_final.sql

use mediscan_pos;

-- ============================================
-- USERS (10)
-- ============================================
insert into users (username, password, first_name, last_name, middle_initial, role, created_by) values
('admin', 'admin123', 'Maria', 'Santos', 'D.', 'admin', null),
('jcruz', 'pass123', 'Juan', 'Cruz', 'P.', 'pharmacist', 1),
('lgomez', 'pass123', 'Liza', 'Gomez', 'R.', 'cashier', 1),
('rlopez', 'pass123', 'Rico', 'Lopez', 'A.', 'cashier', 1),
('avillar', 'pass123', 'Ana', 'Villareal', 'M.', 'pharmacist', 1),
('bdelacruz', 'pass123', 'Ben', 'Dela Cruz', 'S.', 'cashier', 3),
('ctan', 'pass123', 'Carla', 'Tan', 'L.', 'cashier', 3),
('dmendoza', 'pass123', 'Dave', 'Mendoza', 'J.', 'pharmacist', 1),
('efernandez', 'pass123', 'Elena', 'Fernandez', 'B.', 'cashier', 3),
('fsantiago', 'pass123', 'Fred', 'Santiago', 'C.', 'admin', 1);

-- ============================================
-- CATEGORIES (10)
-- ============================================
insert into categories (category_name) values
('Antibiotics'),
('Pain Relievers'),
('Cold & Flu'),
('Vitamins'),
('Nicotine Products'),
('Antacids'),
('Antihistamines'),
('Cough Preparations'),
('Topical / Skin Care'),
('First Aid Supplies');

-- ============================================
-- PRODUCTS (10)
-- ============================================
insert into products (name, brand, category_id, cost_price, selling_price, requires_prescription, is_age_restricted, minimum_age, unit_type, pack_size) values
('Amoxicillin 500mg', 'Amoxil', 1, 5.00, 8.50, true, false, null, 'capsule', 21),
('Biogesic 500mg', 'Biogesic', 2, 2.00, 3.50, false, false, null, 'tablet', 100),
('Neozep Forte', 'Neozep', 3, 4.00, 6.50, false, true, 18, 'tablet', 20),
('Vitamin C 500mg', 'Cecon', 4, 3.00, 5.00, false, false, null, 'tablet', 100),
('Nicotine Gum 2mg', 'Nicorette', 5, 6.00, 10.00, false, true, 18, 'piece', 30),
('Kremil-S', 'Kremil-S', 6, 3.50, 5.50, false, false, null, 'tablet', 100),
('Cetirizine 10mg', 'Virlix', 7, 2.00, 3.50, false, false, null, 'tablet', 50),
('Robitussin Syrup 60ml', 'Robitussin', 8, 5.00, 8.00, false, false, null, 'ml', 60),
('Betadine Solution 60ml', 'Betadine', 9, 4.50, 7.00, false, false, null, 'ml', 60),
('Bandage Roll 5cm', 'Medline', 10, 1.50, 3.00, false, false, null, 'piece', null);

-- ============================================
-- PRODUCT STOCK (10)
-- ============================================
insert into product_stock (product_id, batch_no, quantity, expiry_date, date_received) values
(1, 'B-1001', 50, '2027-05-01', '2026-06-01'),
(2, 'B-1002', 200, '2028-01-15', '2026-05-10'),
(3, 'B-1003', 80, '2026-12-01', '2026-06-15'),
(4, 'B-1004', 150, '2027-08-20', '2026-06-01'),
(5, 'B-1005', 60, '2027-03-10', '2026-06-20'),
(6, 'B-1006', 90, '2027-06-15', '2026-06-10'),
(7, 'B-1007', 70, '2027-01-25', '2026-06-12'),
(8, 'B-1008', 40, '2026-11-30', '2026-06-18'),
(9, 'B-1009', 100, '2027-09-05', '2026-06-20'),
(10, 'B-1010', 300, '2028-04-10', '2026-06-22');

-- ============================================
-- CUSTOMERS (10)
-- ============================================
insert into customers (first_name, last_name, middle_initial, date_of_birth, email, phone_number, created_by) values
('Anna', 'Reyes', 'T.', '1990-03-15', 'anna.reyes@email.com', '09171234567', 3),
('Mark', 'Villanueva', 'G.', '2005-07-22', 'mark.v@email.com', '09181234567', 3),
('Rose', 'Tan', 'F.', '1985-11-30', 'rose.tan@email.com', '09191234567', 4),
('Paolo', 'Ramos', 'E.', '1998-02-10', 'paolo.ramos@email.com', '09201234567', 3),
('Grace', 'Uy', 'M.', '1975-09-05', 'grace.uy@email.com', '09211234567', 4),
('Kevin', 'Sy', 'D.', '2000-12-19', 'kevin.sy@email.com', '09221234567', 6),
('Nina', 'Bautista', 'A.', '1993-06-28', 'nina.bautista@email.com', '09231234567', 6),
('Oscar', 'Manalo', 'R.', '1988-04-14', 'oscar.manalo@email.com', '09241234567', 7),
('Trisha', 'Aquino', 'P.', '2002-10-01', 'trisha.aquino@email.com', '09251234567', 7),
('Leo', 'Navarro', 'C.', '1979-01-23', 'leo.navarro@email.com', '09261234567', 9);

-- ============================================
-- PRESCRIPTIONS (10)
-- ============================================
insert into prescriptions (customer_id, product_id, doctor_name, dosage, quantity_prescribed, date_issued, status) values
(1, 1, 'Dr. Ramon Lopez', '500mg 3x daily', 21, '2026-07-15', 'ready'),
(2, 1, 'Dr. Ramon Lopez', '500mg 2x daily', 14, '2026-07-18', 'pending'),
(3, 3, 'Dr. Elisa Ong', '1 tab every 6 hrs', 10, '2026-07-10', 'picked_up'),
(4, 1, 'Dr. Ramon Lopez', '500mg 3x daily', 21, '2026-07-20', 'ready'),
(5, 3, 'Dr. Elisa Ong', '1 tab every 8 hrs', 12, '2026-07-22', 'pending'),
(6, 1, 'Dr. Mika Reyes', '500mg 2x daily', 14, '2026-07-25', 'pending'),
(7, 3, 'Dr. Elisa Ong', '1 tab every 6 hrs', 10, '2026-07-26', 'ready'),
(8, 1, 'Dr. Mika Reyes', '500mg 3x daily', 21, '2026-07-27', 'picked_up'),
(9, 3, 'Dr. Ramon Lopez', '1 tab every 8 hrs', 12, '2026-07-28', 'pending'),
(10, 1, 'Dr. Mika Reyes', '500mg 2x daily', 14, '2026-07-29', 'ready');

-- ============================================
-- REFILL REMINDERS (10)
-- ============================================
insert into refill_reminders (prescription_id, reminder_date, method, sent_status) values
(1, '2026-08-01', 'sms', 'pending'),
(2, '2026-08-02', 'email', 'pending'),
(3, '2026-08-03', 'sms', 'sent'),
(4, '2026-08-04', 'email', 'pending'),
(5, '2026-08-05', 'sms', 'pending'),
(6, '2026-08-06', 'email', 'sent'),
(7, '2026-08-07', 'sms', 'pending'),
(8, '2026-08-08', 'email', 'sent'),
(9, '2026-08-09', 'sms', 'pending'),
(10, '2026-08-10', 'email', 'pending');

-- ============================================
-- ORDERS (10)
-- ============================================
insert into orders (user_id, customer_id, total_amount, payment_method) values
(3, 1, 40.00, 'cash'),
(4, 3, 75.00, 'card'),
(3, 2, 27.50, 'cash'),
(6, 1, 68.00, 'insurance'),
(3, null, 20.00, 'cash'),
(4, 3, 20.00, 'card'),
(6, 2, 30.00, 'cash'),
(3, 4, 15.00, 'cash'),
(4, 5, 33.00, 'card'),
(6, null, 12.50, 'cash');

-- ============================================
-- ORDER ITEMS (10) -- one line item per order, subtotal matches order total
-- ============================================
insert into order_items (order_id, product_id, quantity, unit_price, subtotal, id_verified) values
(1, 2, 2, 20.00, 40.00, false),
(2, 4, 3, 25.00, 75.00, false),
(3, 7, 5, 5.50, 27.50, false),
(4, 1, 8, 8.50, 68.00, false),
(5, 5, 2, 10.00, 20.00, true),
(6, 6, 4, 5.00, 20.00, false),
(7, 10, 10, 3.00, 30.00, false),
(8, 3, 3, 5.00, 15.00, false),
(9, 8, 3, 11.00, 33.00, false),
(10, 5, 1, 12.50, 12.50, true);