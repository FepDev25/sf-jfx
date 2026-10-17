package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Pedido de venta (pickup o delivery) con flujo de estados. */
public record Pedido(
        Long id,
        long empresaId,
        long sucursalId,
        Long clienteId,
        String clienteNombre,
        String numero,
        String tipoEntrega,
        String estado,
        Long direccionEntregaId,
        String direccionEntrega,
        LocalDateTime fecha,
        LocalDateTime fechaEntregaEstimada,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal total,
        Long ventaId,
        String ventaNumero,
        String nota,
        List<PedidoDetalle> detalles) {

    public boolean convertido() {
        return ventaId != null;
    }
}
