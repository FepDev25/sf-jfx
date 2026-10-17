package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Programa de fidelidad (puntos por compra y canje). */
public record ProgramaFidelidad(
        Long id,
        long empresaId,
        String nombre,
        boolean activo,
        BigDecimal puntosPorMonto,
        BigDecimal valorPunto,
        Integer vigenciaPuntosDias) {
}
