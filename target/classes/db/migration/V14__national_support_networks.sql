-- Réseaux nationaux de soutien (gérés par l'admin, visibles dans Groupes de soutien)
CREATE TABLE pelycan.national_support_networks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    subtitle VARCHAR(300),
    description TEXT,
    website_url VARCHAR(500),
    icon VARCHAR(64) NOT NULL DEFAULT 'volunteer_activism',
    color VARCHAR(32) NOT NULL DEFAULT 'purple',
    services TEXT,
    order_index INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_national_support_networks_active
    ON pelycan.national_support_networks (is_active, order_index);
