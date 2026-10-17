package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Detalle de una toma de inventario. */
public record TomaDetalle(
        Long id,
        long tomaId,
        long productoId,
        String productoNombre,
        Long loteId,
        BigDecimal cantidadSistema,
        BigDecimal cantidadContada,
        BigDecimal diferencia,
        BigDecimal costoUnitario) {
}
