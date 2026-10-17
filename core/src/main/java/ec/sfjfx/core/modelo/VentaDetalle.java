package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de venta. */
public record VentaDetalle(
        Long id,
        long ventaId,
        Long productoId,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        Long impuestoId,
        BigDecimal impuestoValor,
        BigDecimal subtotal,
        BigDecimal total,
        BigDecimal costoUnitario) {
}
