-- ── FILE: src/main/resources/db/migration/V1__init_schema.sql ──
-- Initial database schema for Finance Dashboard
-- Managed by Flyway. DO NOT manually edit applied migrations.

-- ─────────────────────────────────────────────
-- EXTENSION: pgcrypto for gen_random_uuid()
-- (available in PostgreSQL 13+ natively via gen_random_uuid())
-- ─────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────
-- TABLE: users
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
                                     id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL
    CHECK (role IN ('VIEWER', 'ANALYST', 'ADMIN')),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
    );

-- Index for fast email lookups (login, duplicate-check)
CREATE INDEX IF NOT EXISTS idx_users_email
    ON users (email);

-- Index for role-based queries
CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);

-- ─────────────────────────────────────────────
-- TABLE: transactions
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
                                            id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL
    REFERENCES users (id) ON DELETE CASCADE,
    amount           NUMERIC(15,2) NOT NULL
    CHECK (amount > 0),
    type             VARCHAR(50)   NOT NULL
    CHECK (type IN ('INCOME', 'EXPENSE')),
    category         VARCHAR(100)  NOT NULL
    CHECK (category IN (
           'SALARY', 'FOOD', 'RENT', 'UTILITIES',
           'ENTERTAINMENT', 'HEALTHCARE', 'TRANSPORT',
           'INVESTMENT', 'OTHER'
                       )),
    transaction_date DATE          NOT NULL,
    description      TEXT,
    is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
    );

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_transactions_user_id
    ON transactions (user_id);

CREATE INDEX IF NOT EXISTS idx_transactions_type
    ON transactions (type);

CREATE INDEX IF NOT EXISTS idx_transactions_category
    ON transactions (category);

CREATE INDEX IF NOT EXISTS idx_transactions_date
    ON transactions (transaction_date);

CREATE INDEX IF NOT EXISTS idx_transactions_is_deleted
    ON transactions (is_deleted);

-- Composite index for dashboard queries (user + date range + not deleted)
CREATE INDEX IF NOT EXISTS idx_transactions_user_date_active
    ON transactions (user_id, transaction_date, is_deleted);

-- ─────────────────────────────────────────────
-- FUNCTION: auto-update updated_at on row change
-- ─────────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to users
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Attach trigger to transactions
CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();