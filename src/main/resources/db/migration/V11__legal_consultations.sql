CREATE TABLE pelycan.legal_consultations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES pelycan.users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'en_attente',
    modalite VARCHAR(50) NOT NULL,
    urgence BOOLEAN NOT NULL DEFAULT FALSE,
    date_consultation TIMESTAMPTZ,
    avocat_nom VARCHAR(255),
    avocat_email VARCHAR(255),
    avocat_telephone VARCHAR(50),
    avocat_specialite VARCHAR(255),
    notes TEXT,
    notes_internes TEXT,
    assigned_to UUID REFERENCES pelycan.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_legal_consultations_user ON pelycan.legal_consultations (user_id);
CREATE INDEX idx_legal_consultations_status ON pelycan.legal_consultations (status);
