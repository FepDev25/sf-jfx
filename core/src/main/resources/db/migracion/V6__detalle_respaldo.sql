-- =============================================================================
-- V6 — Detalle de respaldos
-- Permite guardar el resultado (error o resumen) de cada ejecucion.
-- =============================================================================

ALTER TABLE respaldo ADD COLUMN detalle VARCHAR(500);
