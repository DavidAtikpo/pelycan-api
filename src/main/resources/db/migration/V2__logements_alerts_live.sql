CREATE TABLE pelycan.logements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    proprietaire_id UUID NOT NULL REFERENCES pelycan.users (id),
    titre VARCHAR(255) NOT NULL,
    adresse VARCHAR(255) NOT NULL,
    code_postal VARCHAR(20),
    ville VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    capacite INT NOT NULL,
    surface INT NOT NULL,
    prix INT NOT NULL DEFAULT 0,
    telephone VARCHAR(30),
    email VARCHAR(100),
    niveau_securite VARCHAR(20) NOT NULL DEFAULT 'standard',
    pieces INT,
    salles_de_bain INT,
    etage INT,
    description TEXT,
    equipements JSONB NOT NULL DEFAULT '{}',
    disponibilite BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'disponible',
    photos TEXT[] NOT NULL DEFAULT '{}',
    type_hebergement VARCHAR(32) NOT NULL DEFAULT 'permanent',
    date_debut TIMESTAMPTZ,
    date_fin TIMESTAMPTZ,
    conditions_temporaire TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_logements_status ON pelycan.logements (status);
CREATE INDEX idx_logements_proprietaire ON pelycan.logements (proprietaire_id);

CREATE TABLE pelycan.user_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES pelycan.users (id),
    message TEXT NOT NULL,
    location VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    status VARCHAR(16) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'resolved', 'cancelled')),
    resolved_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_alerts_user_status ON pelycan.user_alerts (user_id, status);
CREATE INDEX idx_user_alerts_status ON pelycan.user_alerts (status);

CREATE TABLE pelycan.alert_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id UUID NOT NULL REFERENCES pelycan.user_alerts (id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pelycan.emergency_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES pelycan.users (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    relation VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pelycan.live_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    groupe_parole_id UUID,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'waiting'
        CHECK (status IN ('waiting', 'live', 'ended')),
    host_id UUID REFERENCES pelycan.users (id),
    host_name VARCHAR(255),
    host_role VARCHAR(50),
    max_participants INT NOT NULL DEFAULT 20,
    is_audio_only BOOLEAN NOT NULL DEFAULT FALSE,
    agora_channel_id VARCHAR(255),
    start_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pelycan.session_participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES pelycan.live_sessions (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES pelycan.users (id),
    agora_uid BIGINT,
    user_name VARCHAR(255),
    user_role VARCHAR(50) NOT NULL DEFAULT 'user',
    is_audio_muted BOOLEAN NOT NULL DEFAULT FALSE,
    is_video_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_hand_raised BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at TIMESTAMPTZ
);

CREATE INDEX idx_session_participants_session ON pelycan.session_participants (session_id);
