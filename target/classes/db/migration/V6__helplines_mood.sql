CREATE TABLE pelycan.helplines (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    hours VARCHAR(255) NOT NULL,
    icon VARCHAR(255) NOT NULL DEFAULT 'support_agent',
    color VARCHAR(255) NOT NULL DEFAULT 'purple',
    type VARCHAR(50) NOT NULL DEFAULT 'general',
    is_emergency BOOLEAN NOT NULL DEFAULT false,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_helplines_active_order ON pelycan.helplines (is_active, display_order);

INSERT INTO pelycan.helplines (title, phone_number, description, hours, icon, color, type, is_emergency, display_order, is_active)
VALUES
    ('Urgence Psychologique', '3114', 'Numéro national de prévention du suicide - Écoute, soutien et orientation 24h/24', '24h/24 - 7j/7 - Gratuit', 'crisis_alert', 'orange', 'emergency', true, 0, true),
    ('Violences Femmes Info', '3919', 'Écoute, information et orientation pour toutes les femmes victimes de violences', 'Du lundi au vendredi de 9h à 22h, samedi, dimanche et jours fériés de 9h à 18h', 'support_agent', 'purple', 'specialized', true, 1, true),
    ('SOS Amitié', '09 72 39 40 50', 'Écoute bienveillante et anonyme, soutien émotionnel', '24h/24 - 7j/7', 'favorite', 'pink', 'general', false, 2, true),
    ('Suicide Écoute', '01 45 39 40 00', 'Prévention du suicide, écoute et soutien', '24h/24 - 7j/7', 'psychology_alt', 'indigo', 'specialized', false, 3, true);

CREATE TABLE pelycan.mood_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES pelycan.users (id) ON DELETE CASCADE,
    mood VARCHAR(50) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mood_entries_user_created ON pelycan.mood_entries (user_id, created_at DESC);

CREATE TABLE pelycan.consultation_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES pelycan.users (id) ON DELETE CASCADE,
    professional_id UUID NOT NULL REFERENCES pelycan.users (id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    preferred_date VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_requests_professional ON pelycan.consultation_requests (professional_id, created_at DESC);
