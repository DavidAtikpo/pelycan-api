ALTER TABLE pelycan.consultation_requests
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS priority VARCHAR(50) NOT NULL DEFAULT 'medium',
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE TABLE pelycan.professional_appointments (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL REFERENCES pelycan.users(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES pelycan.users(id) ON DELETE CASCADE,
    appointment_date TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    title VARCHAR(255),
    notes TEXT,
    location VARCHAR(255),
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_professional_appointments_pro
    ON pelycan.professional_appointments (professional_id, appointment_date DESC);

CREATE INDEX idx_professional_appointments_user
    ON pelycan.professional_appointments (user_id, appointment_date DESC);

CREATE TABLE pelycan.professional_availabilities (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL REFERENCES pelycan.users(id) ON DELETE CASCADE,
    availability_type VARCHAR(50) NOT NULL,
    day_of_week INTEGER,
    start_time VARCHAR(10),
    end_time VARCHAR(10),
    start_date DATE,
    end_date DATE,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_pattern VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_professional_availabilities_pro
    ON pelycan.professional_availabilities (professional_id, availability_type);

CREATE TABLE pelycan.pro_case_notes (
    id UUID PRIMARY KEY,
    case_id VARCHAR(100) NOT NULL,
    case_type VARCHAR(50) NOT NULL,
    professional_id UUID NOT NULL REFERENCES pelycan.users(id) ON DELETE CASCADE,
    note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pro_case_notes_case
    ON pelycan.pro_case_notes (case_id, created_at DESC);
