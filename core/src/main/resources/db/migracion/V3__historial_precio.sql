-- =============================================================================
-- V3 — Historial de precios y costos
-- Trazabilidad de cambios de precio (por lista) y costo por producto.
-- =============================================================================

CREATE TABLE historial_precio (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT NOT NULL REFERENCES empresa(id),
    producto_id    BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    lista_precio_id BIGINT REFERENCES lista_precio(id),
    tipo           VARCHAR(10) NOT NULL CHECK (tipo IN ('PRECIO','COSTO')),
    valor_anterior NUMERIC(14,4),
    valor_nuevo    NUMERIC(14,4),
    sucursal_id    BIGINT REFERENCES sucursal(id),
    usuario_id     BIGINT REFERENCES usuario(id),
    creado_en      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_historial_precio_producto ON historial_precio(producto_id, creado_en);
