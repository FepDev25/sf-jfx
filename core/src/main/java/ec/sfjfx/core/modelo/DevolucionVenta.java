package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Devolucion de venta (total o parcial). El comprobante se engancha en el Hito 7. */
public record DevolucionVenta(
        Long id,
        long empresaId,
        long ventaId,
        String ventaNumero,
        Long clienteId,
        String numero,
        LocalDateTime fecha,
        String motivo,
        BigDecimal total,
        Long bodegaId,
        String bodega,
        String estado,
        List<DevolucionVentaDetalle> detalles) {
}
