CREATE TABLE pelycan.user_notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES pelycan.users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'general',
    data JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sender_id UUID,
    sender_name VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_user_notifications_user_created
    ON pelycan.user_notifications (user_id, created_at DESC);

CREATE INDEX idx_user_notifications_user_unread
    ON pelycan.user_notifications (user_id)
    WHERE is_read = FALSE;
