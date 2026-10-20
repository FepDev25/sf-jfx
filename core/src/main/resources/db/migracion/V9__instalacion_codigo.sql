-- =============================================================================
-- V9 — Identificador unico de instalacion para licencias
-- =============================================================================

ALTER TABLE instalacion ADD COLUMN codigo_unico VARCHAR(64);
UPDATE instalacion SET codigo_unico = gen_random_uuid()::text WHERE codigo_unico IS NULL;
ALTER TABLE instalacion ALTER COLUMN codigo_unico SET NOT NULL;
ALTER TABLE instalacion ALTER COLUMN codigo_unico SET DEFAULT gen_random_uuid()::text;
ALTER TABLE instalacion ADD CONSTRAINT uq_instalacion_codigo UNIQUE (codigo_unico);
