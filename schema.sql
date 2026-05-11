-- Cashtrack Enterprise Database Schema

-- 1. Security & Auth (Managed by JPA, but provided for reference)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(36),
    roles VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 2. Core Banking
CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(36) PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    kyc_details TEXT,
    balance DECIMAL(19,4) DEFAULT 0.0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) PRIMARY KEY,
    source_account_id VARCHAR(36),
    target_account_id VARCHAR(36),
    atm_id VARCHAR(36),
    amount DECIMAL(19,4) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- WITHDRAWAL, DEPOSIT, TRANSFER
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, REVERSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_account_id) REFERENCES accounts(id)
);

-- 3. ATM Management
CREATE TABLE IF NOT EXISTS atms (
    id VARCHAR(36) PRIMARY KEY,
    location VARCHAR(255) NOT NULL,
    cash_balance DECIMAL(19,4) DEFAULT 0.0,
    status VARCHAR(20) DEFAULT 'ACTIVE' -- ACTIVE, OUT_OF_SERVICE, MAINTENANCE
);

-- 4. Reconciliation
CREATE TABLE IF NOT EXISTS atm_journals (
    id VARCHAR(36) PRIMARY KEY,
    atm_id VARCHAR(36),
    transaction_id VARCHAR(36),
    amount DECIMAL(19,4) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (atm_id) REFERENCES atms(id)
);

CREATE TABLE IF NOT EXISTS reconciliation_reports (
    id VARCHAR(36) PRIMARY KEY,
    reconcile_date DATE NOT NULL,
    total_transactions INT,
    matched_count INT,
    mismatch_count INT,
    status VARCHAR(20), -- BALANCED, DISCREPANCY
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Analytics & Forecast
CREATE TABLE IF NOT EXISTS analytics_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    metric_name VARCHAR(50) NOT NULL, -- DAILY_VOLUME, FAILURE_RATE, REVENUE
    metric_value DECIMAL(19,4) NOT NULL
);
