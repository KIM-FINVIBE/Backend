-- Seed account used by the frontend local-login button.
-- This migration is intentionally idempotent so it also fixes existing local Docker volumes.
-- Login: student@universion.local / finvest1234!

INSERT INTO users (
    user_id, email, login_id, password_hash, nickname, name, birth_date, phone_number, is_active, created_at, updated_at
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    'student@universion.local',
    'student',
    'pbkdf2_sha256$390000$RmluVmliZVN0dWRlbnQwMQ==$VGjtPPlBdagp51K5MpRW0DrXN4rUoyP7Evbf9ryh1yo=',
    'UniversionTestUser',
    '유니버전 테스트',
    '2001-01-01',
    '010-0000-0000',
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    is_active = 1,
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO wallets (
    user_id, cash_balance_krw, reserved_cash_krw, withdrawable_cash_krw, version_no, created_at, updated_at
)
SELECT
    user_id, 50000000, 0, 50000000, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM users
WHERE email = 'student@universion.local'
ON DUPLICATE KEY UPDATE
    cash_balance_krw = VALUES(cash_balance_krw),
    reserved_cash_krw = VALUES(reserved_cash_krw),
    withdrawable_cash_krw = VALUES(withdrawable_cash_krw),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO wallet_ledger (
    wallet_id, user_id, entry_type, direction, amount_krw, balance_after_krw, reference_type, reference_id, memo, created_at
)
SELECT
    w.wallet_id,
    u.user_id,
    'DEPOSIT',
    'IN',
    50000000,
    50000000,
    'SEED',
    'frontend-login-seed',
    '프론트 로컬 테스트 계정 초기 가상 투자금 지급',
    CURRENT_TIMESTAMP(6)
FROM users u
JOIN wallets w ON w.user_id = u.user_id
WHERE u.email = 'student@universion.local'
  AND NOT EXISTS (
      SELECT 1
      FROM wallet_ledger wl
      WHERE wl.user_id = u.user_id
        AND wl.reference_type = 'SEED'
        AND wl.reference_id = 'frontend-login-seed'
  );

INSERT INTO folders (folder_id, user_id, name, color, created_at, updated_at)
SELECT 'student-default', user_id, '기본', '#42d6ba', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM users
WHERE email = 'student@universion.local'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    color = VALUES(color),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO portfolios (portfolio_id, user_id, name, created_at, updated_at)
SELECT 'student-p1', user_id, '테스트 포트폴리오', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM users
WHERE email = 'student@universion.local'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO favorite_stocks (user_id, stock_id, created_at)
SELECT u.user_id, '1', CURRENT_TIMESTAMP(6)
FROM users u
JOIN stocks s ON s.stock_id = '1'
WHERE u.email = 'student@universion.local'
    ON DUPLICATE KEY UPDATE
                         created_at = favorite_stocks.created_at;
