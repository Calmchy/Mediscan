-- mediscan pos - sample data (taken from your current mediscan_pos dump)
-- run this AFTER db.sql

use mediscan_pos;

-- ============================================
-- USERS
-- ============================================
INSERT INTO `users` (`user_id`, `username`, `password`, `first_name`, `last_name`, `middle_initial`, `role`, `created_by`, `created_at`, `must_change_password`) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Maria', 'Santos', 'D.', 'admin', NULL, '2026-09-24 17:46:33', 0),
(2, 'jcruz', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Juan', 'Cruz', 'P.', 'pharmacist', 1, '2026-09-24 17:46:33', 0),
(3, 'lgomez', 'ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f', 'Liza', 'Gomez', 'R.', 'cashier', 1, '2026-09-24 17:46:33', 0),
(4, 'rlopez', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Rico', 'Lopez', 'A.', 'cashier', 1, '2026-09-24 17:46:33', 1),
(5, 'avillar', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Ana', 'Villareal', 'M.', 'pharmacist', 1, '2026-09-24 17:46:33', 0),
(6, 'bdelacruz', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Ben', 'Dela Cruz', 'S.', 'cashier', 3, '2026-09-24 17:46:33', 0),
(7, 'ctan', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Carla', 'Tan', 'L.', 'cashier', 3, '2026-09-24 17:46:33', 0),
(8, 'dmendoza', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Dave', 'Mendoza', 'J.', 'pharmacist', 1, '2026-09-24 17:46:33', 0),
(9, 'efernandez', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Elena', 'Fernandez', 'B.', 'cashier', 3, '2026-09-24 17:46:33', 0),
(10, 'fsantiago', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Fred', 'Santiago', 'C.', 'admin', 1, '2026-09-24 17:46:33', 0);

-- ============================================
-- CATEGORIES
-- ============================================
INSERT INTO `categories` (`category_id`, `category_name`) VALUES
(6, 'Antacids'),
(1, 'Antibiotics'),
(7, 'Antihistamines'),
(3, 'Cold & Flu'),
(8, 'Cough Preparations'),
(10, 'First Aid Supplies'),
(5, 'Nicotine Products'),
(2, 'Pain Relievers'),
(9, 'Topical / Skin Care'),
(4, 'Vitamins');

-- ============================================
-- PRODUCTS
-- ============================================
INSERT INTO `products` (`product_id`, `name`, `brand`, `category_id`, `cost_price`, `selling_price`, `requires_prescription`, `is_age_restricted`, `minimum_age`, `unit_type`, `pack_size`) VALUES
(1, 'Amoxicillin 500mg', 'Amoxil', 1, 5.00, 8.50, 1, 0, NULL, 'capsule', 21),
(2, 'Biogesic 500mg', 'Biogesic', 2, 2.00, 3.50, 0, 0, NULL, 'tablet', 100),
(3, 'Neozep Forte', 'Neozep', 3, 4.00, 6.50, 0, 1, 18, 'tablet', 20),
(4, 'Vitamin C 500mg', 'Cecon', 4, 3.00, 5.00, 0, 0, NULL, 'tablet', 100),
(5, 'Nicotine Gum 2mg', 'Nicorette', 5, 6.00, 10.00, 0, 1, 18, 'piece', 30),
(6, 'Kremil-S', 'Kremil-S', 6, 3.50, 5.50, 0, 0, NULL, 'tablet', 100),
(7, 'Cetirizine 10mg', 'Virlix', 7, 2.00, 3.50, 0, 0, NULL, 'tablet', 50),
(8, 'Robitussin Syrup 60ml', 'Robitussin', 8, 5.00, 8.00, 0, 0, NULL, 'ml', 60),
(9, 'Betadine Solution 60ml', 'Betadine', 9, 4.50, 7.00, 0, 0, NULL, 'ml', 60),
(10, 'Bandage Roll 5cm', 'Medline', 10, 1.50, 3.00, 0, 0, NULL, 'piece', NULL);

-- ============================================
-- PRODUCT_STOCK
-- ============================================
INSERT INTO `product_stock` (`stock_id`, `product_id`, `batch_no`, `quantity`, `expiry_date`, `date_received`) VALUES
(1, 1, 'B-1001', 39, '2027-05-01', '2026-06-01'),
(2, 2, 'B-1002', 192, '2028-01-15', '2026-05-10'),
(3, 3, 'B-1003', 75, '2026-12-01', '2026-06-15'),
(4, 4, 'B-1004', 145, '2027-08-20', '2026-06-01'),
(5, 5, 'B-1005', 58, '2027-03-10', '2026-06-20'),
(6, 6, 'B-1006', 86, '2027-06-15', '2026-06-10'),
(7, 7, 'B-1007', 64, '2027-01-25', '2026-06-12'),
(8, 8, 'B-1008', 34, '2026-11-30', '2026-06-18'),
(9, 9, 'B-1009', 96, '2027-09-05', '2026-06-20'),
(10, 10, 'B-1010', 298, '2028-04-10', '2026-06-22');

-- ============================================
-- CUSTOMERS
-- ============================================
INSERT INTO `customers` (`customer_id`, `first_name`, `last_name`, `middle_initial`, `date_of_birth`, `email`, `phone_number`, `created_by`, `created_at`) VALUES
(1, 'Anna', 'Reyes', 'T.', '1990-03-15', 'anna.reyes@email.com', '09171234567', 3, '2026-09-24 17:46:33'),
(2, 'Mark', 'Villanueva', 'G.', '2005-07-22', 'mark.v@email.com', '09181234567', 3, '2026-09-24 17:46:33'),
(3, 'Rose', 'Tan', 'F.', '1985-11-30', 'rose.tan@email.com', '09191234567', 4, '2026-09-24 17:46:33'),
(4, 'Paolo', 'Ramos', 'E.', '1998-02-10', 'paolo.ramos@email.com', '09201234567', 3, '2026-09-24 17:46:33'),
(5, 'Grace', 'Uy', 'M.', '1975-09-05', 'grace.uy@email.com', '09211234567', 4, '2026-09-24 17:46:33'),
(6, 'Kevin', 'Sy', 'D.', '2000-12-19', 'kevin.sy@email.com', '09221234567', 6, '2026-09-24 17:46:33'),
(7, 'Nina', 'Bautista', 'A.', '1993-06-28', 'nina.bautista@email.com', '09231234567', 6, '2026-09-24 17:46:33'),
(8, 'Oscar', 'Manalo', 'R.', '1988-04-14', 'oscar.manalo@email.com', '09241234567', 7, '2026-09-24 17:46:33'),
(9, 'Trisha', 'Aquino', 'P.', '2002-10-01', 'trisha.aquino@email.com', '09251234567', 7, '2026-09-24 17:46:33'),
(10, 'Leo', 'Navarro', 'C.', '1979-01-23', 'leo.navarro@email.com', '09261234567', 9, '2026-09-24 17:46:33');

-- ============================================
-- PRESCRIPTIONS
-- ============================================
INSERT INTO `prescriptions` (`prescription_id`, `customer_id`, `product_id`, `doctor_name`, `dosage`, `quantity_prescribed`, `date_issued`, `status`) VALUES
(1, 1, 1, 'Dr. Ramon Lopez', '500mg 3x daily', 21, '2026-07-15', 'ready'),
(2, 2, 1, 'Dr. Ramon Lopez', '500mg 2x daily', 14, '2026-07-18', 'pending'),
(3, 3, 3, 'Dr. Elisa Ong', '1 tab every 6 hrs', 10, '2026-07-10', 'picked_up'),
(4, 4, 1, 'Dr. Ramon Lopez', '500mg 3x daily', 21, '2026-07-20', 'ready'),
(5, 5, 3, 'Dr. Elisa Ong', '1 tab every 8 hrs', 12, '2026-07-22', 'pending'),
(6, 6, 1, 'Dr. Mika Reyes', '500mg 2x daily', 14, '2026-07-25', 'pending'),
(7, 7, 3, 'Dr. Elisa Ong', '1 tab every 6 hrs', 10, '2026-07-26', 'ready'),
(8, 8, 1, 'Dr. Mika Reyes', '500mg 3x daily', 21, '2026-07-27', 'picked_up'),
(9, 9, 3, 'Dr. Ramon Lopez', '1 tab every 8 hrs', 12, '2026-07-28', 'pending'),
(10, 10, 1, 'Dr. Mika Reyes', '500mg 2x daily', 14, '2026-07-29', 'ready');

-- ============================================
-- REFILL_REMINDERS
-- ============================================
INSERT INTO `refill_reminders` (`reminder_id`, `prescription_id`, `reminder_date`, `method`, `sent_status`) VALUES
(1, 1, '2026-08-01', 'sms', 'pending'),
(2, 2, '2026-08-02', 'email', 'pending'),
(3, 3, '2026-08-03', 'sms', 'sent'),
(4, 4, '2026-08-04', 'email', 'pending'),
(5, 5, '2026-08-05', 'sms', 'pending'),
(6, 6, '2026-08-06', 'email', 'sent'),
(7, 7, '2026-08-07', 'sms', 'pending'),
(8, 8, '2026-08-08', 'email', 'sent'),
(9, 9, '2026-08-09', 'sms', 'pending'),
(10, 10, '2026-08-10', 'email', 'pending');

-- ============================================
-- ORDERS
-- ============================================
INSERT INTO `orders` (`order_id`, `user_id`, `customer_id`, `order_date`, `total_amount`) VALUES
(1, 3, 1, '2026-09-24 17:46:33', 40.00),
(2, 4, 3, '2026-09-24 17:46:33', 75.00),
(3, 3, 2, '2026-09-24 17:46:33', 27.50),
(4, 6, 1, '2026-09-24 17:46:33', 68.00),
(5, 3, NULL, '2026-09-24 17:46:33', 20.00),
(6, 4, 3, '2026-09-24 17:46:33', 20.00),
(7, 6, 2, '2026-09-24 17:46:33', 30.00),
(8, 3, 4, '2026-09-24 17:46:33', 15.00),
(9, 4, 5, '2026-09-24 17:46:33', 33.00),
(10, 6, NULL, '2026-09-24 17:46:33', 12.50);

-- ============================================
-- ORDER_ITEMS
-- ============================================
INSERT INTO `order_items` (`order_item_id`, `order_id`, `product_id`, `quantity`, `unit_price`, `subtotal`, `id_verified`) VALUES
(1, 1, 2, 2, 20.00, 40.00, 0),
(2, 2, 4, 3, 25.00, 75.00, 0),
(3, 3, 7, 5, 5.50, 27.50, 0),
(4, 4, 1, 8, 8.50, 68.00, 0),
(5, 5, 5, 2, 10.00, 20.00, 1),
(6, 6, 6, 4, 5.00, 20.00, 0),
(7, 7, 10, 10, 3.00, 30.00, 0),
(8, 8, 3, 3, 5.00, 15.00, 0),
(9, 9, 8, 3, 11.00, 33.00, 0),
(10, 10, 5, 1, 12.50, 12.50, 1);