CREATE TABLE pelycan.groupe_parole_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    session_type VARCHAR(20) NOT NULL DEFAULT 'visio'
        CHECK (session_type IN ('visio', 'presentiel')),
    session_date DATE NOT NULL,
    session_time VARCHAR(16) NOT NULL DEFAULT '00:00',
    location VARCHAR(255) NOT NULL DEFAULT 'En ligne',
    max_participants INT NOT NULL DEFAULT 15,
    current_participants INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ouvert',
    animateur_name VARCHAR(255),
    price DECIMAL(10, 2),
    created_by UUID REFERENCES pelycan.users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_groupe_parole_sessions_date ON pelycan.groupe_parole_sessions (session_date DESC);
CREATE INDEX idx_groupe_parole_sessions_status ON pelycan.groupe_parole_sessions (status);
