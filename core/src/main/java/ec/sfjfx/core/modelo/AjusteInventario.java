package ec.sfjfx.core.modelo;

import java.time.LocalDateTime;
import java.util.List;

/** Ajuste de inventario (entrada o salida con motivo). */
public record AjusteInventario(
        Long id,
        long empresaId,
        long bodegaId,
        String bodega,
        String numero,
        String tipo,
        String motivo,
        String estado,
        LocalDateTime fecha,
        String nota,
        List<AjusteDetalle> detalles) {
}
