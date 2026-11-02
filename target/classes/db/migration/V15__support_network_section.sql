ALTER TABLE pelycan.national_support_networks
    ADD COLUMN IF NOT EXISTS section VARCHAR(32) NOT NULL DEFAULT 'national';

CREATE INDEX IF NOT EXISTS idx_national_support_networks_section
    ON pelycan.national_support_networks (section, is_active, order_index);
