package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Detalle de una transferencia. */
public record TransferenciaDetalle(
        Long id,
        long transferenciaId,
        long productoId,
        String productoNombre,
        Long loteId,
        BigDecimal cantidadEnviada,
        BigDecimal cantidadRecibida) {
}
