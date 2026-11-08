use mediscan_pos;

-- ============================================
-- USERS
-- ============================================
-- Password is set to admin123
INSERT INTO `users` (`username`, `password`, `first_name`, `last_name`, `middle_initial`, `role`, `created_by`, `created_at`, `must_change_password`) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Maria', 'Santos', 'D.', 'admin', NULL, '2026-09-24 17:46:33', 0);