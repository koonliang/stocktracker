-- Demo user (password: password123)
-- BCrypt hash for 'password123'
INSERT INTO users (name, email, password, enabled, role, created_at, updated_at)
VALUES (
    'Demo User',
    'demo@stocktracker.com',
    '$2a$10$rS.FGqk9S6yNqX8N8fNQO.WK4YfxG5QnJqQCFHOvJvqEK0hLiKkCi',
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
    '$2a$10$dN.5C3H9oJQ1qJHd0X0vYOGN0q5rB4G7vS0hL8F9mQT4kP5xL2cZy',
    true,
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
