-- SLA Profiles table
CREATE TABLE IF NOT EXISTS sla_profile (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    definition_json CLOB NOT NULL,       -- SLADefinition as JSON
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- SLA Rules table (for rule-engine mode)
CREATE TABLE IF NOT EXISTS sla_rule (
    id VARCHAR(64) PRIMARY KEY,
    priority INT NOT NULL DEFAULT 0,
    condition_json CLOB NOT NULL,      -- Condition as JSON
    action_json CLOB NOT NULL,         -- SLAAction as JSON
    active BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Production Calendar
CREATE TABLE IF NOT EXISTS holiday (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    name VARCHAR(255),
    type VARCHAR(32) NOT NULL,         -- PUBLIC / TRANSFERRED / SHORTENED
    zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    UNIQUE (date, zone)
);

CREATE INDEX idx_holiday_date ON holiday(date);
CREATE INDEX idx_holiday_zone ON holiday(zone);
