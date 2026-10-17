package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimiento de cuenta por cobrar (cargo o abono). */
public record CxcMovimiento(
        Long id,
        long empresaId,
        long clienteId,
        String clienteNombre,
        Long ventaId,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoResultante,
        Long medioPagoId,
        LocalDateTime fecha,
        String nota) {
}
