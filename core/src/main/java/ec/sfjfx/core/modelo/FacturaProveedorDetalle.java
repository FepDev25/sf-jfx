package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de factura de proveedor. */
public record FacturaProveedorDetalle(
        Long id,
        long facturaProveedorId,
        Long productoId,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        Long impuestoId,
        BigDecimal total) {
}
