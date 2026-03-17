-- ============================================
-- FRAUD DETECTION SYSTEM DATABASE (PostgreSQL)
-- ============================================

-- Run these statements from psql if the database does not exist yet:
-- CREATE DATABASE fraud_detection_system;
-- \c fraud_detection_system
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TYPE transaction_type_enum AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'PAYMENT', 'OTHER');

-- ============================================
-- ADMIN TABLE
-- ============================================

CREATE TABLE admin (
    admin_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- BANK EMPLOYEES
-- ============================================

CREATE TABLE employees (
    employee_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(120) UNIQUE,
    role VARCHAR(50),
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CUSTOMERS
-- ============================================

CREATE TABLE customers (
    customer_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_user_id BIGINT UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120),
    phone VARCHAR(15),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    password VARCHAR(255),
    username VARCHAR(100) UNIQUE
);

-- ============================================
-- AUDITORS (INVESTIGATE FRAUD)
-- ============================================

CREATE TABLE auditors (
    auditor_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(120) UNIQUE,
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CUSTOMER ACCOUNTS
-- ============================================

CREATE TABLE accounts (
    account_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_account_id BIGINT UNIQUE,
    customer_id INT,
    account_number VARCHAR(20) UNIQUE,
    account_type VARCHAR(50),
    balance DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- ============================================
-- TRANSACTIONS (DEPOSIT / WITHDRAW / TRANSFER)
-- ============================================

CREATE TABLE transactions (
    transaction_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_transaction_id BIGINT UNIQUE,
    account_id INT,
    counterparty_account_id INT,
    transaction_type transaction_type_enum NOT NULL,
    amount DECIMAL(15,2),
    channel VARCHAR(30),
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    initiated_by INT,
    status VARCHAR(50),
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT fk_transactions_counterparty
        FOREIGN KEY (counterparty_account_id) REFERENCES accounts(account_id),
    CONSTRAINT fk_transactions_employee
        FOREIGN KEY (initiated_by) REFERENCES employees(employee_id)
);

-- ============================================
-- FRAUD DETECTION TABLE
-- ============================================

CREATE TABLE fraud_transactions (
    fraud_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id INT,
    fraud_type VARCHAR(100),
    fraud_reason TEXT,
    risk_score INT,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fraud_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

CREATE TABLE anomaly_investigation_queue (
    queue_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id INT UNIQUE,
    customer_id INT,
    risk_score INT,
    reason TEXT,
    status VARCHAR(30) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_queue_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    CONSTRAINT fk_queue_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

    CREATE TABLE admin_activity_logs (
        log_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        admin_id INT,
        action VARCHAR(120) NOT NULL,
        details TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT fk_admin_activity_admin
        FOREIGN KEY (admin_id) REFERENCES admin(admin_id)
    );

-- ============================================
-- FRAUD INVESTIGATION
-- ============================================

CREATE TABLE fraud_investigation (
    investigation_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fraud_id INT,
    auditor_id INT,
    investigation_status VARCHAR(50),
    remarks TEXT,
    investigated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_investigation_fraud
        FOREIGN KEY (fraud_id) REFERENCES fraud_transactions(fraud_id),
    CONSTRAINT fk_investigation_auditor
        FOREIGN KEY (auditor_id) REFERENCES auditors(auditor_id)
);

-- ============================================
-- EMPLOYEE ACTIVITY LOG
-- ============================================

CREATE TABLE employee_logs (
    log_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id INT,
    action_type VARCHAR(100),
    description TEXT,
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_employee
        FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
);
