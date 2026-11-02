CREATE TABLE pelycan.relationship_evaluations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES pelycan.users(id) ON DELETE CASCADE,
    relation_name VARCHAR(255) NOT NULL,
    relation_type VARCHAR(50) NOT NULL,
    evaluation_date TIMESTAMPTZ NOT NULL,
    category_scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    total_score INTEGER NOT NULL DEFAULT 0,
    max_score INTEGER NOT NULL DEFAULT 100,
    toxicity_level VARCHAR(50) NOT NULL,
    answers JSONB NOT NULL DEFAULT '{}'::jsonb,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_relationship_evaluations_user
    ON pelycan.relationship_evaluations (user_id, evaluation_date DESC);

CREATE INDEX idx_relationship_evaluations_user_relation
    ON pelycan.relationship_evaluations (user_id, relation_name);
