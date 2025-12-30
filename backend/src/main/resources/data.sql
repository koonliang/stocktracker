-- Demo user (password: password123)
-- BCrypt hash for 'password123'
-- INSERT IGNORE will skip if user already exists
-- Explicitly set id=1 for demo user
INSERT IGNORE INTO users (id, name, email, password, enabled, role, created_at, updated_at)
VALUES (
    1,
    'Demo User',
    'demo@stocktracker.com',
    '$2b$10$UnijFso2x5Rjuu6wcoDHMu694i2Tz4GDC.X5BwAYKy2L/g1uLfvMy',
    true,
    'USER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Admin user (password: admin123)
-- Explicitly set id=2 for admin user
INSERT IGNORE INTO users (id, name, email, password, enabled, role, created_at, updated_at)
VALUES (
    2,
    'Admin User',
    'admin@stocktracker.com',
    '$2b$10$DA5Jl5UBW2mXv683j8jiIOUf5/EOQ2sObeUBZ0cPzWfUPGeMJzLV2',
    true,
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Sample holdings for demo user (id = 1)
-- INSERT IGNORE will skip if holdings already exist
INSERT IGNORE INTO holdings (user_id, symbol, company_name, shares, average_cost, created_at, updated_at)
VALUES
    (1, 'AAPL', 'Apple Inc.', 50.0000, 142.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'MSFT', 'Microsoft Corporation', 25.0000, 285.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'GOOGL', 'Alphabet Inc.', 10.0000, 125.30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'TSLA', 'Tesla, Inc.', 15.0000, 248.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'NVDA', 'NVIDIA Corporation', 20.0000, 450.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'AMZN', 'Amazon.com, Inc.', 30.0000, 135.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
