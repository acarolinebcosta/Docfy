CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_users_email UNIQUE (email),

    CONSTRAINT ck_users_role
        CHECK (role IN ('ADMIN', 'MANAGER', 'COLLABORATOR'))
);