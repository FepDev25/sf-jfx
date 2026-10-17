package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Direccion de un cliente. */
public record ClienteDireccion(
        Long id,
        long clienteId,
        String alias,
        String direccion,
        String referencia,
        BigDecimal lat,
        BigDecimal lng,
        boolean esPrincipal) {
}
