package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de devolucion de venta. */
public record DevolucionVentaDetalle(
        Long id,
        long devolucionId,
        long productoId,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal total,
        boolean reingresaStock) {
}
