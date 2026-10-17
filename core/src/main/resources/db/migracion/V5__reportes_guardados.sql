-- =============================================================================
-- V5 — Reportes guardados
-- Configuraciones de filtros reutilizables por usuario y empresa.
-- =============================================================================

CREATE TABLE reporte_guardado (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    usuario_id     BIGINT REFERENCES usuario(id),
    codigo         VARCHAR(60) NOT NULL,
    nombre         VARCHAR(120) NOT NULL,
    desde          DATE,
    hasta          DATE,
    sucursal_id    BIGINT REFERENCES sucursal(id),
    categoria_id   BIGINT REFERENCES categoria(id),
    filtro_usuario_id BIGINT REFERENCES usuario(id),
    creado_en      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reporte_guardado_empresa ON reporte_guardado(empresa_id, codigo);
