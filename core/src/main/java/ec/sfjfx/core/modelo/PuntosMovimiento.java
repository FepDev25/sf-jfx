package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimiento de puntos de fidelidad. */
public record PuntosMovimiento(
        Long id,
        long clienteId,
        String clienteNombre,
        String tipo,
        BigDecimal puntos,
        String referenciaTipo,
        Long referenciaId,
        BigDecimal saldoResultante,
        LocalDateTime fecha,
        String nota) {
}
