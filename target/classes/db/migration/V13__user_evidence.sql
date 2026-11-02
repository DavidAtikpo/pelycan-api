CREATE TABLE pelycan.user_evidence (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES pelycan.users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_url TEXT NOT NULL,
    file_size INTEGER,
    mime_type VARCHAR(100),
    description TEXT,
    incident_date TIMESTAMPTZ,
    location TEXT,
    encrypted BOOLEAN NOT NULL DEFAULT TRUE,
    shared_with JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_evidence_user ON pelycan.user_evidence (user_id);
CREATE INDEX idx_user_evidence_type ON pelycan.user_evidence (user_id, type);
