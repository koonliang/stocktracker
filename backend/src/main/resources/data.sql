-- Demo user (password: password123)
-- BCrypt hash for 'password123'
INSERT INTO users (name, email, password, enabled, role, created_at, updated_at)
VALUES (
    'Demo User',
    'demo@stocktracker.com',
    '$2b$10$UnijFso2x5Rjuu6wcoDHMu694i2Tz4GDC.X5BwAYKy2L/g1uLfvMy',
    true,
    'USER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Admin user (password: admin123)
INSERT INTO users (name, email, password, enabled, role, created_at, updated_at)
VALUES (
    'Admin User',
    'admin@stocktracker.com',
    '$2b$10$DA5Jl5UBW2mXv683j8jiIOUf5/EOQ2sObeUBZ0cPzWfUPGeMJzLV2',
    true,
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
