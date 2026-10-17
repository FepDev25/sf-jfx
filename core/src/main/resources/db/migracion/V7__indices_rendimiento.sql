-- =============================================================================
-- V7 — Indices de rendimiento para listados y reportes
-- Cubren filtros por empresa, sucursal, estado y fecha usados por la UI y el
-- modulo de reportes, ademas de paginacion por orden descendente.
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_venta_empresa_estado_fecha ON venta(empresa_id, estado, fecha);
CREATE INDEX IF NOT EXISTS idx_venta_sucursal_fecha ON venta(sucursal_id, fecha);
CREATE INDEX IF NOT EXISTS idx_venta_usuario_fecha ON venta(usuario_id, fecha);
CREATE INDEX IF NOT EXISTS idx_venta_detalle_producto ON venta_detalle(producto_id);
CREATE INDEX IF NOT EXISTS idx_venta_pago_medio ON venta_pago(medio_pago_id);

CREATE INDEX IF NOT EXISTS idx_producto_empresa_activo ON producto(empresa_id, activo, nombre);
CREATE INDEX IF NOT EXISTS idx_cliente_empresa_activo ON cliente(empresa_id, activo, razon_social);
CREATE INDEX IF NOT EXISTS idx_proveedor_empresa_activo ON proveedor(empresa_id, activo, razon_social);

CREATE INDEX IF NOT EXISTS idx_stock_bodega_producto ON stock(bodega_id, producto_id);
CREATE INDEX IF NOT EXISTS idx_mov_inv_empresa_fecha ON movimiento_inventario(empresa_id, creado_en);
CREATE INDEX IF NOT EXISTS idx_lote_producto_vencimiento ON lote(producto_id, fecha_vencimiento);

CREATE INDEX IF NOT EXISTS idx_factura_prov_empresa_estado ON factura_proveedor(empresa_id, estado_pago, fecha_emision);
CREATE INDEX IF NOT EXISTS idx_cxc_empresa_fecha ON cxc_movimiento(empresa_id, creado_en);
CREATE INDEX IF NOT EXISTS idx_comprobante_empresa_fecha ON comprobante(empresa_id, fecha_emision);
CREATE INDEX IF NOT EXISTS idx_retencion_empresa_fecha ON retencion(empresa_id, fecha_emision);
CREATE INDEX IF NOT EXISTS idx_auditoria_empresa_fecha ON auditoria(empresa_id, creado_en);
CREATE INDEX IF NOT EXISTS idx_respaldo_empresa_fecha ON respaldo(empresa_id, creado_en);
