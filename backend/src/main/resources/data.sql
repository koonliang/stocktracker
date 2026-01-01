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

-- Sample transactions for demo user (id = 1)
-- These transactions will automatically generate the holdings through HoldingRecalculationService
-- Transactions are ordered by date to show realistic trading history
-- Only insert if transactions table is empty to avoid duplicates on server restart

-- AAPL Transactions: Buy 60 shares, Sell 10 shares = 50 shares net @ avg $142.50
INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'BUY', 'AAPL', 'Apple Inc.', '2024-01-15', 60.0000, 142.50, 8550.00, 'Initial AAPL purchase', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 LIMIT 1);

INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'SELL', 'AAPL', 'Apple Inc.', '2024-06-20', 10.0000, 150.00, 1500.00, 'Partial profit taking', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'AAPL' AND type = 'SELL' LIMIT 1);

-- MSFT Transactions: Buy 30 shares, Sell 5 shares = 25 shares net @ avg $285.00
INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'BUY', 'MSFT', 'Microsoft Corporation', '2024-02-01', 30.0000, 285.00, 8550.00, 'MSFT position', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'MSFT' LIMIT 1);

INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'SELL', 'MSFT', 'Microsoft Corporation', '2024-07-15', 5.0000, 320.00, 1600.00, 'Rebalancing portfolio', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'MSFT' AND type = 'SELL' LIMIT 1);

-- GOOGL Transactions: Buy 10 shares @ $125.30
INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'BUY', 'GOOGL', 'Alphabet Inc.', '2024-03-10', 10.0000, 125.30, 1253.00, 'GOOGL investment', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'GOOGL' LIMIT 1);

-- TSLA Transactions: Buy 20 shares, Sell 5 shares = 15 shares net @ avg $248.00
INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'BUY', 'TSLA', 'Tesla, Inc.', '2024-04-05', 20.0000, 248.00, 4960.00, 'TSLA entry', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'TSLA' LIMIT 1);

INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'SELL', 'TSLA', 'Tesla, Inc.', '2024-08-10', 5.0000, 265.00, 1325.00, 'Lock in gains', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'TSLA' AND type = 'SELL' LIMIT 1);

-- NVDA Transactions: Buy 20 shares @ $450.00
INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'BUY', 'NVDA', 'NVIDIA Corporation', '2024-05-20', 20.0000, 450.00, 9000.00, 'AI chip play', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'NVDA' LIMIT 1);

-- AMZN Transactions: Buy 40 shares, Sell 10 shares = 30 shares net @ avg $135.00
INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'BUY', 'AMZN', 'Amazon.com, Inc.', '2024-01-20', 40.0000, 135.00, 5400.00, 'AMZN investment', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'AMZN' LIMIT 1);

INSERT INTO transactions (user_id, type, symbol, company_name, transaction_date, shares, price_per_share, total_amount, notes, created_at, updated_at)
SELECT 1, 'SELL', 'AMZN', 'Amazon.com, Inc.', '2024-09-01', 10.0000, 145.00, 1450.00, 'Portfolio rebalance', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE user_id = 1 AND symbol = 'AMZN' AND type = 'SELL' LIMIT 1);

-- Sample holdings for demo user (id = 1)
-- Note: These will be automatically recalculated from transactions by HoldingRecalculationService
-- Keeping these for backward compatibility with existing code that might expect initial holdings
INSERT IGNORE INTO holdings (user_id, symbol, company_name, shares, average_cost, created_at, updated_at)
VALUES
    (1, 'AAPL', 'Apple Inc.', 50.0000, 142.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'MSFT', 'Microsoft Corporation', 25.0000, 285.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'GOOGL', 'Alphabet Inc.', 10.0000, 125.30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'TSLA', 'Tesla, Inc.', 15.0000, 248.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'NVDA', 'NVIDIA Corporation', 20.0000, 450.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'AMZN', 'Amazon.com, Inc.', 30.0000, 135.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
