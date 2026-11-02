-- Contacts de secours publics (gérés par l'admin, visibles par tous)
CREATE TABLE pelycan.public_emergency_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL DEFAULT 'other',
    type VARCHAR(32) NOT NULL DEFAULT 'emergency',
    order_index INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_public_emergency_contacts_active
    ON pelycan.public_emergency_contacts (is_active, order_index);
