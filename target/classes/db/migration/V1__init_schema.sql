CREATE SCHEMA IF NOT EXISTS pelycan;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE pelycan.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    phone_number VARCHAR(50),
    google_id VARCHAR(255) UNIQUE,
    role VARCHAR(32) NOT NULL DEFAULT 'user'
        CHECK (role IN ('user', 'pro', 'admin', 'bailleur', 'partner')),
    status VARCHAR(32) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'pending', 'inactive', 'banned')),
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON pelycan.users (email);

CREATE TABLE pelycan.actualites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(300) NOT NULL,
    excerpt TEXT NOT NULL,
    content TEXT,
    category VARCHAR(32) NOT NULL DEFAULT 'plateforme'
        CHECK (category IN ('plateforme', 'sensibilisation', 'ressources')),
    status VARCHAR(16) NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft', 'published')),
    published_at TIMESTAMPTZ,
    link_url VARCHAR(500),
    link_label VARCHAR(200),
    created_by UUID NOT NULL REFERENCES pelycan.users (id),
    updated_by UUID REFERENCES pelycan.users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_actualites_status_published ON pelycan.actualites (status, published_at DESC);

-- Admin par défaut (mot de passe: Admin123! — à changer en prod)
INSERT INTO pelycan.users (id, first_name, last_name, email, password, role, status)
VALUES (
    '00000000-0000-4000-8000-000000000001',
    'Admin',
    'Pelycan',
    'admin@pelycan.local',
    '$2a$10$rcl2tHzgOWB0EJBgoZ7ZcOPyir9i/c0pG4NsHum9UQyxmAnzIEuZO',
    'admin',
    'active'
);
