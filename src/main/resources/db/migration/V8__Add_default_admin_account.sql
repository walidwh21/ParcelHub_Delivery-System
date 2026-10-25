-- Seeds the initial administrator. Email and password hash are injected through
-- Flyway placeholders (ADMIN_EMAIL / ADMIN_PASSWORD_HASH). Without a supplied hash the
-- account is created with a non-matching hash, i.e. it cannot sign in until configured.
INSERT INTO users (id, full_name, email, password, created_at, updated_at)
VALUES (
           '00000000-0000-4000-8000-000000000001',
           'System Administrator',
           '${admin_email}',
           '${admin_password_hash}',
           NOW(),
           NOW()
       )
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role)
VALUES (
           '00000000-0000-4000-8000-000000000001',
           'ROLE_ADMIN'
       );
