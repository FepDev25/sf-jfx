-- =============================================================================
-- V8 — Metadatos de la instalacion
-- Single-tenant auto-instalable: una base PostgreSQL por instalacion.
-- La fila se crea desde el asistente de primer arranque (Fase 1).
-- =============================================================================

CREATE TABLE instalacion (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    modo            VARCHAR(20) NOT NULL DEFAULT 'UNICA_EMPRESA'
                    CHECK (modo IN ('UNICA_EMPRESA', 'MULTIEMPRESA')),
    version_esquema VARCHAR(20),
    creado_en       TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMP
);
