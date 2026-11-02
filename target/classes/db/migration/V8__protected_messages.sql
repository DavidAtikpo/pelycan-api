CREATE TABLE pelycan.protected_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id UUID NOT NULL REFERENCES pelycan.users (id),
    recipient_id UUID NOT NULL REFERENCES pelycan.users (id),
    content TEXT NOT NULL,
    encrypted BOOLEAN NOT NULL DEFAULT TRUE,
    filtered BOOLEAN NOT NULL DEFAULT FALSE,
    filter_reason TEXT,
    inappropriate BOOLEAN NOT NULL DEFAULT FALSE,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    attachments JSONB NOT NULL DEFAULT '[]',
    deleted_by_sender BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_protected_messages_sender_recipient
    ON pelycan.protected_messages (sender_id, recipient_id, created_at);

CREATE INDEX idx_protected_messages_recipient_read
    ON pelycan.protected_messages (recipient_id, is_read);
