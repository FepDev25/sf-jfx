package ec.sfjfx.core.modelo;

import java.time.LocalDateTime;
import java.util.List;

/** Recepcion de compra (parcial o total). */
public record RecepcionCompra(
        Long id,
        long empresaId,
        Long ordenCompraId,
        String numero,
        long bodegaId,
        String bodega,
        LocalDateTime fecha,
        String estado,
        String nota,
        List<RecepcionDetalle> detalles) {
}
