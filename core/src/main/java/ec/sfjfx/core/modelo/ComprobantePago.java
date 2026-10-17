package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Forma de pago de un comprobante (codigo SRI). */
public record ComprobantePago(
        String formaPagoCodigo,
        BigDecimal total) {
}
