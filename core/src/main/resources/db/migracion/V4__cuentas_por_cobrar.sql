-- =============================================================================
-- V4 — Cuentas por cobrar de clientes
-- Movimientos de cargo (venta a credito) y abono (pago del cliente).
-- El saldo vigente se mantiene en cliente.saldo.
-- =============================================================================

CREATE TABLE cxc_movimiento (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        BIGINT NOT NULL REFERENCES empresa(id),
    cliente_id        BIGINT NOT NULL REFERENCES cliente(id),
    venta_id          BIGINT REFERENCES venta(id),
    tipo              VARCHAR(10) NOT NULL CHECK (tipo IN ('CARGO','ABONO','AJUSTE')),
    monto             NUMERIC(14,4) NOT NULL,
    saldo_resultante  NUMERIC(14,4) NOT NULL,
    medio_pago_id     BIGINT REFERENCES medio_pago(id),
    usuario_id        BIGINT REFERENCES usuario(id),
    nota              VARCHAR(255),
    creado_en         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_cxc_cliente ON cxc_movimiento(cliente_id, creado_en);
