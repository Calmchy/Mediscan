-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Nov 10, 2026 at 08:58 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `mediscan_pos`
--
CREATE DATABASE IF NOT EXISTS `mediscan_pos` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `mediscan_pos`;

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

CREATE TABLE `categories` (
  `category_id` int(11) NOT NULL,
  `category_name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `categories`
--

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

-- --------------------------------------------------------

--
-- Table structure for table `customers`
--

CREATE TABLE `customers` (
  `customer_id` int(11) NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `middle_initial` char(50) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `created_by` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `customers`
--

INSERT INTO `customers` (`customer_id`, `first_name`, `last_name`, `middle_initial`, `date_of_birth`, `email`, `phone_number`, `created_by`, `created_at`) VALUES
(1, 'Anna', 'Reyes', 'T.', '1990-03-15', 'anna.reyes@email.com', '09171234567', 3, '2026-09-24 09:46:33'),
(2, 'Mark', 'Villanueva', 'G.', '2005-07-22', 'mark.v@email.com', '09181234567', 3, '2026-09-24 09:46:33'),
(3, 'Rose', 'Tan', 'F.', '1985-11-30', 'rose.tan@email.com', '09191234567', 4, '2026-09-24 09:46:33'),
(4, 'Paolo', 'Ramos', 'E.', '1998-02-10', 'paolo.ramos@email.com', '09201234567', 3, '2026-09-24 09:46:33'),
(5, 'Grace', 'Uy', 'M.', '1975-09-05', 'grace.uy@email.com', '09211234567', 4, '2026-09-24 09:46:33'),
(6, 'Kevin', 'Sy', 'D.', '2000-12-19', 'kevin.sy@email.com', '09221234567', 6, '2026-09-24 09:46:33'),
(7, 'Nina', 'Bautista', 'A.', '1993-06-28', 'nina.bautista@email.com', '09231234567', 6, '2026-09-24 09:46:33'),
(8, 'Oscar', 'Manalo', 'R.', '1988-04-14', 'oscar.manalo@email.com', '09241234567', 7, '2026-09-24 09:46:33'),
(9, 'Trisha', 'Aquino', 'P.', '2002-10-01', 'trisha.aquino@email.com', '09251234567', 7, '2026-09-24 09:46:33'),
(10, 'Leo', 'Navarro', 'C.', '1979-01-23', 'leo.navarro@email.com', '09261234567', 9, '2026-09-24 09:46:33');

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `order_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `customer_id` int(11) DEFAULT NULL,
  `order_type` enum('walk_in','pickup') NOT NULL DEFAULT 'walk_in',
  `order_date` datetime DEFAULT current_timestamp(),
  `total_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `payment_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `change_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` enum('completed','voided') NOT NULL DEFAULT 'completed',
  `void_reason` varchar(255) DEFAULT NULL,
  `voided_by` int(11) DEFAULT NULL,
  `voided_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `orders`
--

INSERT INTO `orders` (`order_id`, `user_id`, `customer_id`, `order_type`, `order_date`, `total_amount`, `payment_amount`, `change_amount`, `status`, `void_reason`, `voided_by`, `voided_at`) VALUES
(1, 3, 1, 'walk_in', '2026-09-24 17:46:33', 40.00, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(2, 4, 3, 'walk_in', '2026-09-24 17:46:33', 75.00, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(3, 3, 2, 'walk_in', '2026-09-24 17:46:33', 27.50, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(4, 6, 1, 'walk_in', '2026-09-24 17:46:33', 68.00, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(5, 3, NULL, 'walk_in', '2026-09-24 17:46:33', 20.00, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(6, 4, 3, 'walk_in', '2026-09-24 17:46:33', 20.00, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(7, 6, 2, 'walk_in', '2026-09-24 17:46:33', 30.00, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(8, 3, 4, 'walk_in', '2026-09-24 17:46:33', 15.00, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(9, 4, 5, 'walk_in', '2026-09-24 17:46:33', 33.00, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(10, 6, NULL, 'walk_in', '2026-09-24 17:46:33', 12.50, 0.00, 0.00, 'completed', NULL, NULL, NULL),
(11, 1, NULL, 'walk_in', '2026-11-05 22:58:54', 53.50, 70.00, 16.50, 'completed', NULL, NULL, NULL),
(12, 1, NULL, 'walk_in', '2026-11-08 13:45:24', 150.50, 170.00, 19.50, 'completed', NULL, NULL, NULL),
(13, 1, NULL, 'walk_in', '2026-11-08 22:42:55', 23.50, 50.00, 26.50, 'completed', NULL, NULL, NULL),
(14, 1, NULL, 'walk_in', '2026-11-08 22:45:43', 132.00, 150.00, 18.00, 'completed', NULL, NULL, NULL),
(15, 1, 1, 'walk_in', '2026-11-08 23:51:16', 64.00, 75.00, 11.00, 'completed', NULL, NULL, NULL),
(16, 1, NULL, 'walk_in', '2026-11-08 23:53:44', 8.50, 10.00, 1.50, 'completed', NULL, NULL, NULL),
(17, 1, NULL, 'walk_in', '2026-11-08 23:59:52', 12.00, 15.00, 3.00, 'completed', NULL, NULL, NULL),
(18, 1, NULL, 'walk_in', '2026-11-09 00:01:17', 64.00, 75.00, 11.00, 'completed', NULL, NULL, NULL),
(19, 3, NULL, 'walk_in', '2026-11-09 00:42:17', 29.00, 30.00, 1.00, 'completed', NULL, NULL, NULL),
(20, 3, 1, 'walk_in', '2026-11-09 00:57:20', 35.50, 50.00, 14.50, 'completed', NULL, NULL, NULL),
(21, 1, NULL, 'pickup', '2026-11-09 01:24:01', 8.50, 10.00, 1.50, 'completed', NULL, NULL, NULL),
(22, 1, NULL, 'walk_in', '2026-11-09 01:24:57', 8.50, 10.00, 1.50, 'completed', NULL, NULL, NULL),
(23, 1, NULL, 'walk_in', '2026-11-11 02:31:12', 30.00, 50.00, 20.00, 'completed', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `order_items`
--

CREATE TABLE `order_items` (
  `order_item_id` int(11) NOT NULL,
  `order_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  `id_verified` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `order_items`
--

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
(10, 10, 5, 1, 12.50, 12.50, 1),
(11, 11, 1, 1, 8.50, 8.50, 0),
(12, 11, 2, 1, 3.50, 3.50, 0),
(13, 11, 3, 1, 6.50, 6.50, 0),
(14, 11, 4, 1, 5.00, 5.00, 0),
(15, 11, 5, 1, 10.00, 10.00, 0),
(16, 11, 6, 1, 5.50, 5.50, 0),
(17, 11, 7, 1, 3.50, 3.50, 0),
(18, 11, 8, 1, 8.00, 8.00, 0),
(19, 11, 10, 1, 3.00, 3.00, 0),
(20, 12, 1, 1, 8.50, 8.50, 0),
(21, 12, 2, 1, 3.50, 3.50, 0),
(22, 12, 4, 1, 5.00, 5.00, 0),
(23, 12, 5, 1, 10.00, 10.00, 0),
(24, 12, 6, 13, 5.50, 71.50, 0),
(25, 12, 7, 14, 3.50, 49.00, 0),
(26, 12, 10, 1, 3.00, 3.00, 0),
(27, 13, 1, 1, 8.50, 8.50, 0),
(28, 13, 2, 1, 3.50, 3.50, 0),
(29, 13, 3, 1, 6.50, 6.50, 0),
(30, 13, 4, 1, 5.00, 5.00, 0),
(31, 14, 1, 1, 8.50, 8.50, 0),
(32, 14, 2, 12, 3.50, 42.00, 0),
(33, 14, 3, 1, 6.50, 6.50, 0),
(34, 14, 4, 15, 5.00, 75.00, 0),
(35, 15, 1, 1, 8.50, 8.50, 0),
(36, 15, 2, 11, 3.50, 38.50, 0),
(37, 15, 3, 1, 6.50, 6.50, 0),
(38, 15, 4, 1, 5.00, 5.00, 0),
(39, 15, 6, 1, 5.50, 5.50, 0),
(40, 16, 1, 1, 8.50, 8.50, 0),
(41, 17, 1, 1, 8.50, 8.50, 0),
(42, 17, 2, 1, 3.50, 3.50, 0),
(43, 18, 1, 1, 8.50, 8.50, 0),
(44, 18, 2, 11, 3.50, 38.50, 0),
(45, 18, 3, 1, 6.50, 6.50, 0),
(46, 18, 4, 1, 5.00, 5.00, 0),
(47, 18, 6, 1, 5.50, 5.50, 0),
(48, 19, 1, 1, 8.50, 8.50, 0),
(49, 19, 2, 1, 3.50, 3.50, 0),
(50, 19, 3, 1, 6.50, 6.50, 0),
(51, 19, 4, 1, 5.00, 5.00, 0),
(52, 19, 6, 1, 5.50, 5.50, 0),
(53, 20, 1, 1, 8.50, 8.50, 0),
(54, 20, 2, 1, 3.50, 3.50, 0),
(55, 20, 3, 1, 6.50, 6.50, 0),
(56, 20, 4, 1, 5.00, 5.00, 0),
(57, 20, 6, 1, 5.50, 5.50, 0),
(58, 20, 7, 1, 3.50, 3.50, 0),
(59, 20, 10, 1, 3.00, 3.00, 0),
(60, 21, 1, 1, 8.50, 8.50, 0),
(61, 22, 1, 1, 8.50, 8.50, 0),
(62, 23, 1, 1, 8.50, 8.50, 0),
(63, 23, 2, 1, 3.50, 3.50, 0),
(64, 23, 3, 1, 6.50, 6.50, 0),
(65, 23, 4, 1, 5.00, 5.00, 0),
(66, 23, 7, 1, 3.50, 3.50, 0),
(67, 23, 10, 1, 3.00, 3.00, 0);

-- --------------------------------------------------------

--
-- Table structure for table `order_item_batches`
--

CREATE TABLE `order_item_batches` (
  `id` int(11) NOT NULL,
  `order_item_id` int(11) NOT NULL,
  `stock_id` int(11) DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  `unit_cost` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `order_item_batches`
--

INSERT INTO `order_item_batches` (`id`, `order_item_id`, `stock_id`, `quantity`, `unit_cost`) VALUES
(1, 1, 2, 2, 2.00),
(2, 2, 4, 3, 3.00),
(3, 3, 7, 5, 2.00),
(4, 4, 1, 8, 5.00),
(5, 5, 5, 2, 6.00),
(6, 6, 6, 4, 3.50),
(7, 7, 10, 10, 1.50),
(8, 8, 3, 3, 4.00),
(9, 9, 8, 3, 5.00),
(10, 10, 5, 1, 6.00),
(11, 11, 1, 1, 5.00),
(12, 12, NULL, 1, 33.00),
(13, 13, 3, 1, 4.00),
(14, 14, 4, 1, 3.00),
(15, 15, 5, 1, 6.00),
(16, 16, 6, 1, 3.50),
(17, 17, 7, 1, 2.00),
(18, 18, 8, 1, 5.00),
(19, 19, 10, 1, 1.50),
(20, 20, 1, 1, 5.00),
(21, 21, 2, 1, 2.00),
(22, 22, 4, 1, 3.00),
(23, 23, 5, 1, 6.00),
(24, 24, 6, 13, 3.50),
(25, 25, 7, 14, 2.00),
(26, 26, 10, 1, 1.50),
(27, 27, 1, 1, 5.00),
(28, 28, 2, 1, 2.00),
(29, 29, 3, 1, 4.00),
(30, 30, 4, 1, 3.00),
(31, 31, 1, 1, 5.00),
(32, 32, 2, 12, 2.00),
(33, 33, 3, 1, 4.00),
(34, 34, 4, 15, 3.00),
(35, 35, 1, 1, 5.00),
(36, 36, 2, 11, 2.00),
(37, 37, 3, 1, 4.00),
(38, 38, 4, 1, 3.00),
(39, 39, 6, 1, 3.50),
(40, 40, 1, 1, 5.00),
(41, 41, 1, 1, 5.00),
(42, 42, 2, 1, 2.00),
(43, 43, 1, 1, 5.00),
(44, 44, 2, 11, 2.00),
(45, 45, 3, 1, 4.00),
(46, 46, 4, 1, 3.00),
(47, 47, 6, 1, 3.50),
(48, 48, 1, 1, 5.00),
(49, 49, 2, 1, 2.00),
(50, 50, 3, 1, 4.00),
(51, 51, 4, 1, 3.00),
(52, 52, 6, 1, 3.50),
(53, 53, 1, 1, 5.00),
(54, 54, 2, 1, 2.00),
(55, 55, 3, 1, 4.00),
(56, 56, 4, 1, 3.00),
(57, 57, 6, 1, 3.50),
(58, 58, 7, 1, 2.00),
(59, 59, 10, 1, 1.50),
(60, 60, 1, 1, 5.00),
(61, 61, 1, 1, 5.00),
(62, 62, 1, 1, 5.00),
(63, 63, 2, 1, 2.00),
(64, 64, 3, 1, 4.00),
(65, 65, 4, 1, 3.00),
(66, 66, 7, 1, 2.00),
(67, 67, 10, 1, 1.50);

-- --------------------------------------------------------

--
-- Table structure for table `products`
--

CREATE TABLE `products` (
  `product_id` int(11) NOT NULL,
  `name` varchar(150) NOT NULL,
  `brand` varchar(100) DEFAULT NULL,
  `category_id` int(11) DEFAULT NULL,
  `selling_price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `requires_prescription` tinyint(1) NOT NULL DEFAULT 0,
  `is_age_restricted` tinyint(1) NOT NULL DEFAULT 0,
  `minimum_age` int(11) DEFAULT NULL,
  `unit_type` enum('tablet','capsule','syrup','ml','box','piece') NOT NULL DEFAULT 'piece',
  `pack_size` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `products`
--

INSERT INTO `products` (`product_id`, `name`, `brand`, `category_id`, `selling_price`, `requires_prescription`, `is_age_restricted`, `minimum_age`, `unit_type`, `pack_size`) VALUES
(1, 'Amoxicillin 500mg', 'Amoxil', 1, 8.50, 1, 0, NULL, 'capsule', 21),
(2, 'Biogesic 500mg', 'Biogesic', 2, 3.50, 0, 0, NULL, 'tablet', 100),
(3, 'Neozep Forte', 'Neozep', 3, 6.50, 0, 1, 18, 'tablet', 20),
(4, 'Vitamin C 500mg', 'Cecon', 4, 5.00, 0, 0, NULL, 'tablet', 100),
(5, 'Nicotine Gum 2mg', 'Nicorette', 5, 10.00, 0, 1, 18, 'piece', 30),
(6, 'Kremil-S', 'Kremil-S', 6, 5.50, 0, 0, NULL, 'tablet', 100),
(7, 'Cetirizine 10mg', 'Virlix', 7, 3.50, 0, 0, NULL, 'tablet', 50),
(8, 'Robitussin Syrup 60ml', 'Robitussin', 8, 8.00, 0, 0, NULL, 'ml', 60),
(9, 'Betadine Solution 60ml', 'Betadine', 9, 7.00, 0, 0, NULL, 'ml', 60),
(10, 'Bandage Roll 5cm', 'Medline', 10, 2.00, 0, 0, NULL, 'piece', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `product_price_history`
--

CREATE TABLE `product_price_history` (
  `id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `old_price` decimal(10,2) DEFAULT NULL,
  `new_price` decimal(10,2) NOT NULL,
  `changed_by` int(11) DEFAULT NULL,
  `changed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `product_price_history`
--

INSERT INTO `product_price_history` (`id`, `product_id`, `old_price`, `new_price`, `changed_by`, `changed_at`) VALUES
(2, 10, 3.00, 2.00, 1, '2026-11-10 18:37:26');

-- --------------------------------------------------------

--
-- Table structure for table `product_stock`
--

CREATE TABLE `product_stock` (
  `stock_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `batch_no` varchar(50) DEFAULT NULL,
  `quantity` int(11) NOT NULL DEFAULT 0,
  `cost_price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `expiry_date` date DEFAULT NULL,
  `date_received` date DEFAULT curdate(),
  `status` enum('active','on_hold','recalled') NOT NULL DEFAULT 'active',
  `status_reason` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `product_stock`
--

INSERT INTO `product_stock` (`stock_id`, `product_id`, `batch_no`, `quantity`, `cost_price`, `expiry_date`, `date_received`, `status`, `status_reason`) VALUES
(1, 1, 'B-1001', 88, 5.00, '2027-05-01', '2026-06-01', 'active', NULL),
(2, 2, 'B-1002', 152, 2.00, '2028-01-15', '2026-05-10', 'active', NULL),
(3, 3, 'B-1003', 67, 4.00, '2026-12-01', '2026-06-15', 'active', NULL),
(4, 4, 'B-1004', 122, 3.00, '2027-08-20', '2026-06-01', 'active', NULL),
(5, 5, 'B-1005', 56, 6.00, '2027-03-10', '2026-06-20', 'on_hold', 'try'),
(6, 6, 'B-1006', 68, 3.50, '2027-06-15', '2026-06-10', 'active', NULL),
(7, 7, 'B-1007', 47, 2.00, '2027-01-25', '2026-06-12', 'active', NULL),
(8, 8, 'B-1008', 33, 5.00, '2026-11-30', '2026-06-18', 'on_hold', 'try'),
(9, 9, 'B-1009', 96, 4.50, '2027-09-05', '2026-06-20', 'recalled', 'Manufacturer recall'),
(10, 10, 'B-1010', 294, 1.50, '2028-04-10', '2026-06-22', 'active', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `middle_initial` char(50) DEFAULT NULL,
  `role` enum('admin','pharmacist','cashier') NOT NULL DEFAULT 'cashier',
  `must_change_password` tinyint(1) NOT NULL DEFAULT 1,
  `created_by` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `username`, `password`, `first_name`, `last_name`, `middle_initial`, `role`, `must_change_password`, `created_by`, `created_at`) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Maria', 'Santos', 'D', 'admin', 0, NULL, '2026-09-24 09:46:33'),
(2, 'pharmacist', 'ac9689e2272427085e35b9d3e3e8bed88cb3434828b43b86fc0596cad4c6e270', 'Juan', 'Cruz', 'P', 'pharmacist', 0, 1, '2026-09-24 09:46:33'),
(3, 'cashier', 'ac9689e2272427085e35b9d3e3e8bed88cb3434828b43b86fc0596cad4c6e270', 'Liza', 'Gomez', 'R', 'cashier', 0, 1, '2026-09-24 09:46:33'),
(4, 'rlopez', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Rico', 'Lopez', 'A.', 'cashier', 1, 1, '2026-09-24 09:46:33'),
(5, 'avillar', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Ana', 'Villareal', 'M.', 'pharmacist', 0, 1, '2026-09-24 09:46:33'),
(6, 'bdelacruz', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Ben', 'Dela Cruz', 'S.', 'cashier', 0, 3, '2026-09-24 09:46:33'),
(7, 'ctan', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Carla', 'Tan', 'L.', 'cashier', 0, 3, '2026-09-24 09:46:33'),
(8, 'dmendoza', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Dave', 'Mendoza', 'J.', 'pharmacist', 0, 1, '2026-09-24 09:46:33'),
(9, 'efernandez', '9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', 'Elena', 'Fernandez', 'B.', 'cashier', 0, 3, '2026-09-24 09:46:33'),
(10, 'test@', '8622f0f69c91819119a8acf60a248d7b36fdb7ccf857ba8f85cf7f2767ff8265', 'Fred', 'Santiago', 'C', 'admin', 0, 1, '2026-09-24 09:46:33');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`category_id`),
  ADD UNIQUE KEY `category_name` (`category_name`);

--
-- Indexes for table `customers`
--
ALTER TABLE `customers`
  ADD PRIMARY KEY (`customer_id`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`order_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `customer_id` (`customer_id`),
  ADD KEY `fk_orders_voided_by` (`voided_by`),
  ADD KEY `idx_orders_status_date` (`status`,`order_date`);

--
-- Indexes for table `order_items`
--
ALTER TABLE `order_items`
  ADD PRIMARY KEY (`order_item_id`),
  ADD KEY `order_id` (`order_id`),
  ADD KEY `product_id` (`product_id`);

--
-- Indexes for table `order_item_batches`
--
ALTER TABLE `order_item_batches`
  ADD PRIMARY KEY (`id`),
  ADD KEY `stock_id` (`stock_id`),
  ADD KEY `idx_batches_order_item` (`order_item_id`);

--
-- Indexes for table `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`product_id`),
  ADD KEY `category_id` (`category_id`);

--
-- Indexes for table `product_price_history`
--
ALTER TABLE `product_price_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `changed_by` (`changed_by`),
  ADD KEY `idx_price_history_product` (`product_id`,`changed_at`);

--
-- Indexes for table `product_stock`
--
ALTER TABLE `product_stock`
  ADD PRIMARY KEY (`stock_id`),
  ADD KEY `idx_stock_expiry` (`product_id`,`expiry_date`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `created_by` (`created_by`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `categories`
--
ALTER TABLE `categories`
  MODIFY `category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `customers`
--
ALTER TABLE `customers`
  MODIFY `customer_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `orders`
--
ALTER TABLE `orders`
  MODIFY `order_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- AUTO_INCREMENT for table `order_items`
--
ALTER TABLE `order_items`
  MODIFY `order_item_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=68;

--
-- AUTO_INCREMENT for table `order_item_batches`
--
ALTER TABLE `order_item_batches`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=68;

--
-- AUTO_INCREMENT for table `products`
--
ALTER TABLE `products`
  MODIFY `product_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `product_price_history`
--
ALTER TABLE `product_price_history`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `product_stock`
--
ALTER TABLE `product_stock`
  MODIFY `stock_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `fk_orders_voided_by` FOREIGN KEY (`voided_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `orders_ibfk_2` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `order_items`
--
ALTER TABLE `order_items`
  ADD CONSTRAINT `order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `order_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON UPDATE CASCADE;

--
-- Constraints for table `order_item_batches`
--
ALTER TABLE `order_item_batches`
  ADD CONSTRAINT `order_item_batches_ibfk_1` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`order_item_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `order_item_batches_ibfk_2` FOREIGN KEY (`stock_id`) REFERENCES `product_stock` (`stock_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `products`
--
ALTER TABLE `products`
  ADD CONSTRAINT `products_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `product_price_history`
--
ALTER TABLE `product_price_history`
  ADD CONSTRAINT `product_price_history_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `product_price_history_ibfk_2` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `product_stock`
--
ALTER TABLE `product_stock`
  ADD CONSTRAINT `product_stock_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `users_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
