package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de devolucion a proveedor. */
public record DevolucionCompraDetalle(
        Long id,
        long devolucionId,
        long productoId,
        String productoNombre,
        Long loteId,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal total) {
}
