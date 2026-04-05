-- ── FILE: src/main/resources/db/migration/V2__seed_data.sql ──
-- Seed data for Finance Dashboard.
-- Passwords are BCrypt-hashed (cost=10):
--   admin@finance.com  → Admin@123
--   analyst@finance.com → Analyst@123
--   viewer@finance.com  → Viewer@123

-- ─────────────────────────────────────────────
-- USERS
-- Using fixed UUIDs so transaction foreign keys are deterministic.
-- ─────────────────────────────────────────────
INSERT INTO users (id, email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES
    (
        'a1b2c3d4-0000-0000-0000-000000000001',
        'admin@finance.com',
        -- BCrypt hash of "Admin@123"
        '$2a$10$wCrKabB3mF7vMdzVzpEhLOCMi1mg4suM8yBM1xDMKmBHVEtZ8OMse',
        'System Administrator',
        'ADMIN',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000002',
        'analyst@finance.com',
        -- BCrypt hash of "Analyst@123"
        '$2a$10$Vbl3DQed/9QXL.HGTuxsS.ubYeEs9aiunPN/RZ4Dp1K/BwKAQpA4a',
        'Jane Analyst',
        'ANALYST',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        'a1b2c3d4-0000-0000-0000-000000000003',
        'viewer@finance.com',
        -- BCrypt hash of "Viewer@123"
        '$2a$10$hLXaevDSMpdteQXhfbQrYuA4nUQfZV/ShyE9T0Kv/ez1JPwt798cS',
        'Bob Viewer',
        'VIEWER',
        TRUE,
        NOW(),
        NOW()
    )
    ON CONFLICT (email) DO NOTHING;

-- ─────────────────────────────────────────────
-- TRANSACTIONS — 18 sample records
-- Mix of INCOME / EXPENSE across all categories
-- Spread across the last 6 months
-- ─────────────────────────────────────────────

-- ── Admin's transactions ──
INSERT INTO transactions (id, user_id, amount, type, category, transaction_date, description, is_deleted)
VALUES
    (
        'b1000000-0000-0000-0000-000000000001',
        'a1b2c3d4-0000-0000-0000-000000000001',
        8500.00, 'INCOME', 'SALARY',
        CURRENT_DATE - INTERVAL '1 month',
        'Monthly salary payment - March',
        FALSE
    ),
    (
        'b1000000-0000-0000-0000-000000000002',
        'a1b2c3d4-0000-0000-0000-000000000001',
        1800.00, 'EXPENSE', 'RENT',
        CURRENT_DATE - INTERVAL '1 month',
        'Monthly apartment rent',
        FALSE
    ),
    (
        'b1000000-0000-0000-0000-000000000003',
        'a1b2c3d4-0000-0000-0000-000000000001',
        320.50, 'EXPENSE', 'FOOD',
        CURRENT_DATE - INTERVAL '25 days',
        'Grocery shopping — weekly supplies',
        FALSE
    ),
    (
        'b1000000-0000-0000-0000-000000000004',
        'a1b2c3d4-0000-0000-0000-000000000001',
        5000.00, 'INCOME', 'INVESTMENT',
        CURRENT_DATE - INTERVAL '20 days',
        'Dividend payout — equity portfolio Q1',
        FALSE
    ),
    (
        'b1000000-0000-0000-0000-000000000005',
        'a1b2c3d4-0000-0000-0000-000000000001',
        150.00, 'EXPENSE', 'UTILITIES',
        CURRENT_DATE - INTERVAL '15 days',
        'Electricity and internet bill',
        FALSE
    ),
    (
        'b1000000-0000-0000-0000-000000000006',
        'a1b2c3d4-0000-0000-0000-000000000001',
        85.00, 'EXPENSE', 'ENTERTAINMENT',
        CURRENT_DATE - INTERVAL '10 days',
        'Streaming services and cinema tickets',
        FALSE
    ),
    (
        'b1000000-0000-0000-0000-000000000007',
        'a1b2c3d4-0000-0000-0000-000000000001',
        420.00, 'EXPENSE', 'HEALTHCARE',
        CURRENT_DATE - INTERVAL '8 days',
        'Annual health checkup and dental cleaning',
        FALSE
    ),
    (
        'b1000000-0000-0000-0000-000000000008',
        'a1b2c3d4-0000-0000-0000-000000000001',
        95.00, 'EXPENSE', 'TRANSPORT',
        CURRENT_DATE - INTERVAL '5 days',
        'Monthly bus pass and ride-hailing',
        FALSE
    );

-- ── Analyst's transactions ──
INSERT INTO transactions (id, user_id, amount, type, category, transaction_date, description, is_deleted)
VALUES
    (
        'b2000000-0000-0000-0000-000000000001',
        'a1b2c3d4-0000-0000-0000-000000000002',
        6200.00, 'INCOME', 'SALARY',
        CURRENT_DATE - INTERVAL '1 month',
        'Monthly salary — February',
        FALSE
    ),
    (
        'b2000000-0000-0000-0000-000000000002',
        'a1b2c3d4-0000-0000-0000-000000000002',
        1200.00, 'EXPENSE', 'RENT',
        CURRENT_DATE - INTERVAL '1 month',
        'Rent payment for shared apartment',
        FALSE
    ),
    (
        'b2000000-0000-0000-0000-000000000003',
        'a1b2c3d4-0000-0000-0000-000000000002',
        275.00, 'EXPENSE', 'FOOD',
        CURRENT_DATE - INTERVAL '18 days',
        'Supermarket and restaurant expenses',
        FALSE
    ),
    (
        'b2000000-0000-0000-0000-000000000004',
        'a1b2c3d4-0000-0000-0000-000000000002',
        800.00, 'INCOME', 'OTHER',
        CURRENT_DATE - INTERVAL '12 days',
        'Freelance data analysis project',
        FALSE
    ),
    (
        'b2000000-0000-0000-0000-000000000005',
        'a1b2c3d4-0000-0000-0000-000000000002',
        60.00, 'EXPENSE', 'TRANSPORT',
        CURRENT_DATE - INTERVAL '7 days',
        'Weekly commute — metro card top-up',
        FALSE
    ),
    (
        'b2000000-0000-0000-0000-000000000006',
        'a1b2c3d4-0000-0000-0000-000000000002',
        200.00, 'EXPENSE', 'ENTERTAINMENT',
        CURRENT_DATE - INTERVAL '3 days',
        'Concert tickets and merchandise',
        FALSE
    ),
    -- Soft-deleted transaction (demonstrates is_deleted filter)
    (
        'b2000000-0000-0000-0000-000000000007',
        'a1b2c3d4-0000-0000-0000-000000000002',
        999.99, 'EXPENSE', 'OTHER',
        CURRENT_DATE - INTERVAL '30 days',
        'Duplicate entry — soft deleted',
        TRUE
    );

-- ── Viewer's transactions ──
INSERT INTO transactions (id, user_id, amount, type, category, transaction_date, description, is_deleted)
VALUES
    (
        'b3000000-0000-0000-0000-000000000001',
        'a1b2c3d4-0000-0000-0000-000000000003',
        4500.00, 'INCOME', 'SALARY',
        CURRENT_DATE - INTERVAL '1 month',
        'Monthly salary — junior analyst',
        FALSE
    ),
    (
        'b3000000-0000-0000-0000-000000000002',
        'a1b2c3d4-0000-0000-0000-000000000003',
        900.00, 'EXPENSE', 'RENT',
        CURRENT_DATE - INTERVAL '1 month',
        'Room rent — shared house',
        FALSE
    ),
    (
        'b3000000-0000-0000-0000-000000000003',
        'a1b2c3d4-0000-0000-0000-000000000003',
        180.00, 'EXPENSE', 'FOOD',
        CURRENT_DATE - INTERVAL '14 days',
        'Weekly groceries',
        FALSE
    );

-- End of seed data