CREATE TABLE pelycan.groupe_parole_inscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES pelycan.groupe_parole_sessions (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES pelycan.users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, user_id)
);

CREATE INDEX idx_gp_inscriptions_user ON pelycan.groupe_parole_inscriptions (user_id);
CREATE INDEX idx_gp_inscriptions_session ON pelycan.groupe_parole_inscriptions (session_id);
