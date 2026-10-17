package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de pedido. */
public record PedidoDetalle(
        Long id,
        long pedidoId,
        Long productoId,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal total) {
}
