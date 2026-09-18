-- V2__create_users.sql
--
-- El rol se guarda como texto y el catalogo vive en el enum Role: el CHECK
-- es la red de seguridad del lado de la base, no la fuente de verdad.
--
-- El superadmin se siembra aqui porque una base recien migrada no tiene
-- ninguna otra forma de entrar por la API. Su contrasena inicial es publica
-- (esta en el repositorio): cambiala en el primer arranque de cada entorno.

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT'))
);

CREATE INDEX idx_users_role ON users (role);

INSERT INTO users (id, username, password_hash, role, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'superadmin',
    '{bcrypt}$2a$10$Dny9M/4y84SVioDvQAMO.u6YU9HQh2.cmjSD/rOheszM5pzqXT7XW',
    'SUPER_ADMIN',
    TRUE,
    now(),
    now()
);
