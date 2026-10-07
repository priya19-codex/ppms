-- Optional: the app creates these tables itself (spring.jpa.hibernate.ddl-auto=update).
-- Run this file first if you want to review or create the schema manually.
CREATE DATABASE IF NOT EXISTS paddy_db CHARACTER SET utf8mb4;
USE paddy_db;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  mobile VARCHAR(15) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,            -- BCrypt
  role VARCHAR(20) NOT NULL,                      -- FARMER | STAFF | ADMIN
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  centre_id BIGINT NULL,                          -- staff -> centre
  created_at DATETIME(6)
);
CREATE TABLE farmers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  farmer_id VARCHAR(255) NOT NULL UNIQUE,
  aadhaar_masked VARCHAR(255),                    -- last 4 digits only
  address VARCHAR(255), village VARCHAR(255), district VARCHAR(255), state VARCHAR(255),
  preferred_centre_id BIGINT,
  land_acres DOUBLE NOT NULL DEFAULT 0,
  approved_max_qty INT NOT NULL DEFAULT 0,        -- bags, set by admin
  FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE procurement_centres (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  centre_code VARCHAR(255) NOT NULL UNIQUE,
  centre_name VARCHAR(255) NOT NULL,
  address VARCHAR(255), village VARCHAR(255), district VARCHAR(255), contact_number VARCHAR(255),
  open_time TIME, close_time TIME,
  max_daily_capacity INT NOT NULL DEFAULT 100,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);
CREATE TABLE slots (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  centre_id BIGINT NOT NULL,
  slot_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  capacity INT NOT NULL,
  booked_count INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  UNIQUE KEY uq_slot (centre_id, slot_date, start_time),
  KEY idx_slot_centre_date (centre_id, slot_date),
  CONSTRAINT chk_capacity CHECK (booked_count >= 0 AND booked_count <= capacity),   -- DB-level overbooking guard
  FOREIGN KEY (centre_id) REFERENCES procurement_centres(id)
);
CREATE TABLE bookings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id VARCHAR(255) NOT NULL UNIQUE,
  token_number VARCHAR(255) NOT NULL,
  farmer_id BIGINT NOT NULL, centre_id BIGINT NOT NULL, slot_id BIGINT NOT NULL,
  booking_date DATE NOT NULL,
  vehicle_number VARCHAR(255), vehicle_type VARCHAR(255), paddy_type VARCHAR(255), contact_number VARCHAR(255),
  paddy_quantity INT NOT NULL,
  booking_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
  reminder_sent BIT NOT NULL DEFAULT 0,
  created_at DATETIME(6), updated_at DATETIME(6),
  UNIQUE KEY uq_token (centre_id, booking_date, token_number),
  KEY idx_b_farmer (farmer_id), KEY idx_b_slot (slot_id), KEY idx_b_date (booking_date),
  FOREIGN KEY (farmer_id) REFERENCES farmers(id),
  FOREIGN KEY (centre_id) REFERENCES procurement_centres(id),
  FOREIGN KEY (slot_id) REFERENCES slots(id)
);
CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(255), message VARCHAR(500), type VARCHAR(255),
  is_read BIT NOT NULL DEFAULT 0,
  created_at DATETIME(6),
  KEY idx_n_user (user_id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE quantity_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  farmer_id BIGINT NOT NULL,
  requested_qty INT NOT NULL, reason VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(6),
  FOREIGN KEY (farmer_id) REFERENCES farmers(id)
);
-- Demo data: not SQL. On first start with an empty database, DataSeeder.java inserts 3 centres, 5 farmers,
-- staff, admin, slots for the next days, bookings in every status and notifications, with dates relative to "today"
-- and BCrypt-hashed passwords. See README.
