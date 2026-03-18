CREATE SCHEMA IF NOT EXISTS accounts;
CREATE TABLE IF NOT EXISTS accounts.accounts
(
    id         BIGSERIAL PRIMARY KEY,
    login      VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    birth_date DATE,
    balance    DECIMAL(19, 2) DEFAULT 0.0
);