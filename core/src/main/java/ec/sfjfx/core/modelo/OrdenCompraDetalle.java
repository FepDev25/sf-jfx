package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de orden de compra. */
public record OrdenCompraDetalle(
        Long id,
        long ordenCompraId,
        long productoId,
        String productoNombre,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal cantidadRecibida,
        BigDecimal precioUnitario,
        Long impuestoId,
        BigDecimal descuento,
        BigDecimal subtotal,
        BigDecimal total) {

    public BigDecimal pendiente() {
        BigDecimal c = cantidad == null ? BigDecimal.ZERO : cantidad;
        BigDecimal r = cantidadRecibida == null ? BigDecimal.ZERO : cantidadRecibida;
        return c.subtract(r);
    }
}
