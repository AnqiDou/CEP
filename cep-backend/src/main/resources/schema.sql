DROP TABLE IF EXISTS email_verification_codes;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    email NVARCHAR (100) NOT NULL UNIQUE,
    username NVARCHAR (50),
    password_hash NVARCHAR (100) NOT NULL,
    status NVARCHAR (20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME2 NULL
);

CREATE TABLE email_verification_codes (
    id BIGINT IDENTITY (1, 1) PRIMARY KEY,
    email NVARCHAR (100) NOT NULL,
    purpose NVARCHAR (30) NOT NULL,
    code NVARCHAR (6) NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    expires_at DATETIME2 NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_verification_email_purpose ON email_verification_codes (
    email,
    purpose,
    created_at DESC
);
