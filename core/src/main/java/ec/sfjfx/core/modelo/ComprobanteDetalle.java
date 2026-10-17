package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de un comprobante electronico. */
public record ComprobanteDetalle(
        Long id,
        long comprobanteId,
        String codigoPrincipal,
        String codigoAuxiliar,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal subtotal,
        BigDecimal tarifa,
        BigDecimal impuestoValor,
        BigDecimal total) {
}
