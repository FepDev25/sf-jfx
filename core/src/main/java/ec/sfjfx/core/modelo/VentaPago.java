package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Pago aplicado a una venta (permite pago mixto). */
public record VentaPago(
        Long id,
        long ventaId,
        long medioPagoId,
        String medioPago,
        BigDecimal monto,
        String referencia,
        BigDecimal cambio) {
}
