-- ============================================
-- Stock Tracker Database Initialization Script
-- ============================================
-- This script creates the database and user for Stock Tracker application
-- Run this on your MySQL server BEFORE deploying the application
--
-- Usage:
--   mysql -u root -p < init-database.sql
--
-- OR connect to MySQL and run:
--   source /path/to/init-database.sql
--
-- IMPORTANT: Replace <DB_PASSWORD> with your actual secure password!
-- ============================================

-- Create database with UTF-8 support
CREATE DATABASE IF NOT EXISTS stocktracker
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Create application user
-- Replace <DB_PASSWORD> with your actual password
-- For better security, consider using specific host instead of '%'
-- e.g., 'stocktracker'@'192.168.1.100' instead of 'stocktracker'@'%'
CREATE USER IF NOT EXISTS 'stocktracker'@'%' IDENTIFIED BY '<DB_PASSWORD>';

-- Grant privileges on the stocktracker database
GRANT ALL PRIVILEGES ON stocktracker.* TO 'stocktracker'@'%';

-- Apply changes
FLUSH PRIVILEGES;

-- Verify setup
SELECT User, Host FROM mysql.user WHERE User = 'stocktracker';

-- Show databases
SHOW DATABASES LIKE 'stocktracker';

-- ============================================
-- Security Recommendations
-- ============================================
-- 1. Use a strong password (mix of letters, numbers, symbols)
-- 2. Consider restricting access to specific host(s):
--    Instead of:  'stocktracker'@'%'
--    Use:         'stocktracker'@'192.168.1.100' (your LXC IP)
--
-- 3. After initial setup with ddl-auto=update, change to ddl-auto=validate
--    in application-prod.yml to prevent accidental schema changes
--
-- 4. Consider using MySQL SSL/TLS for encrypted connections
--
-- 5. Regularly backup the database
--
-- ============================================
-- Schema Management
-- ============================================
-- Option 1: Let Hibernate create schema (for initial setup)
--   Set in application-prod.yml: spring.jpa.hibernate.ddl-auto=update
--   After first run, change to: spring.jpa.hibernate.ddl-auto=validate
--
-- Option 2: Use Flyway/Liquibase migrations (recommended for production)
--   Add migration scripts to handle schema versioning
--
-- Option 3: Manual schema creation
--   Export schema from development and run manually
--   Then set: spring.jpa.hibernate.ddl-auto=validate
-- ============================================
