IF OBJECT_ID('dbo.users', 'U') IS NULL BEGIN
CREATE TABLE users (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    email NVARCHAR(255) NOT NULL,
    username NVARCHAR(100) NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

END;
GO

IF OBJECT_ID(
    'dbo.email_verification_codes',
    'U'
) IS NULL BEGIN
CREATE TABLE email_verification_codes (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    email NVARCHAR(255) NOT NULL,
    purpose NVARCHAR(50) NOT NULL,
    code NVARCHAR(10) NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    expires_at DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

END;
GO

IF OBJECT_ID('dbo.auth_sessions', 'U') IS NULL BEGIN
CREATE TABLE auth_sessions (
    id BIGINT IDENTITY(1, 1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token_hash NVARCHAR(255) NOT NULL,
    access_token_hash NVARCHAR(255) NOT NULL,
    refresh_expires_at DATETIME2 NOT NULL,
    access_expires_at DATETIME2 NOT NULL,
    revoked BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'uk_users_email'
        AND object_id = OBJECT_ID('dbo.users')
) BEGIN CREATE UNIQUE
INDEX uk_users_email ON users (email);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'idx_verification_email_purpose'
        AND object_id = OBJECT_ID(
            'dbo.email_verification_codes'
        )
) BEGIN CREATE
INDEX idx_verification_email_purpose ON email_verification_codes (
    email,
    purpose,
    created_at DESC
);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'uk_auth_sessions_refresh_token_hash'
        AND object_id = OBJECT_ID('dbo.auth_sessions')
) BEGIN CREATE UNIQUE
INDEX uk_auth_sessions_refresh_token_hash ON auth_sessions (refresh_token_hash);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'uk_auth_sessions_access_token_hash'
        AND object_id = OBJECT_ID('dbo.auth_sessions')
) BEGIN CREATE UNIQUE
INDEX uk_auth_sessions_access_token_hash ON auth_sessions (access_token_hash);

END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE
        name = 'idx_auth_sessions_user'
        AND object_id = OBJECT_ID('dbo.auth_sessions')
) BEGIN CREATE
INDEX idx_auth_sessions_user ON auth_sessions (user_id, created_at DESC);

END;
GO