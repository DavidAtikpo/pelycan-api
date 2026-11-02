CREATE TABLE pelycan.demandes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    telephone VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    nombre_personnes TEXT NOT NULL,
    niveau_urgence VARCHAR(50) NOT NULL DEFAULT 'normal',
    message TEXT,
    logement_id UUID REFERENCES pelycan.logements (id) ON DELETE SET NULL,
    user_id UUID REFERENCES pelycan.users (id) ON DELETE SET NULL,
    centre_type VARCHAR(100),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'en_attente',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_demandes_logement ON pelycan.demandes (logement_id);
CREATE INDEX idx_demandes_user ON pelycan.demandes (user_id);

CREATE TABLE pelycan.alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES pelycan.users (id),
    user_name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'processed', 'closed')),
    alert_type VARCHAR(50),
    priority VARCHAR(20),
    metadata JSONB NOT NULL DEFAULT '{}',
    messages JSONB NOT NULL DEFAULT '[]',
    viewed_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    alert_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pelycan.user_fcm_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES pelycan.users (id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    platform VARCHAR(20),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, token)
);

CREATE TABLE pelycan.user_medical_info (
    user_id UUID PRIMARY KEY REFERENCES pelycan.users (id) ON DELETE CASCADE,
    blood_type VARCHAR(10),
    allergies TEXT,
    medical_conditions TEXT,
    emergency_notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE pelycan.user_alerts
    ADD COLUMN IF NOT EXISTS contact_ids JSONB,
    ADD COLUMN IF NOT EXISTS recipients JSONB;
