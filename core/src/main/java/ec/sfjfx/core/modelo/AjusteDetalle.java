package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Detalle de un ajuste de inventario. */
public record AjusteDetalle(
        Long id,
        long ajusteId,
        long productoId,
        String productoNombre,
        Long loteId,
        BigDecimal cantidad,
        BigDecimal costoUnitario) {
}
