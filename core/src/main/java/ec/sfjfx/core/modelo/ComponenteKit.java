package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Componente de un kit (producto compuesto). */
public record ComponenteKit(
        Long id,
        long productoKitId,
        long productoComponenteId,
        String componenteNombre,
        BigDecimal cantidad) {
}
