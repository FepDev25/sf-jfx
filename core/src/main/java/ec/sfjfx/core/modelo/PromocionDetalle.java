package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Componente de un combo/pack. */
public record PromocionDetalle(
        Long id,
        long promocionId,
        Long productoId,
        String productoNombre,
        BigDecimal cantidad) {
}
