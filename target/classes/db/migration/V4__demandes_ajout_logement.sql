-- Demandes pour devenir bailleur / publier des logements
CREATE TABLE pelycan.demandes_ajout_logement (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    justificatif VARCHAR(255),
    statut VARCHAR(32) NOT NULL DEFAULT 'en_attente'
        CHECK (statut IN ('en_attente', 'approuvee', 'refusee')),
    date_creation TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    date_mise_a_jour TIMESTAMPTZ,
    raison_demande TEXT NOT NULL,
    raison_refus TEXT,
    est_proprio BOOLEAN NOT NULL DEFAULT FALSE,
    user_id UUID REFERENCES pelycan.users (id) ON DELETE SET NULL,
    bulletins_salaire TEXT[] DEFAULT '{}',
    contrat_travail TEXT[] DEFAULT '{}',
    quittances_loyer TEXT[] DEFAULT '{}',
    justificatif_prise_en_charge TEXT[] DEFAULT '{}',
    piece_identite TEXT[] DEFAULT '{}',
    livret_famille TEXT[] DEFAULT '{}',
    notification_caf TEXT[] DEFAULT '{}',
    numero_piece_identite VARCHAR(255),
    numero_demande_logement VARCHAR(255),
    numero_dalo VARCHAR(255),
    nombre_personnes VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_demandes_ajout_logement_user ON pelycan.demandes_ajout_logement (user_id);
CREATE INDEX idx_demandes_ajout_logement_statut ON pelycan.demandes_ajout_logement (statut);
