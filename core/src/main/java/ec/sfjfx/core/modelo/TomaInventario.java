package ec.sfjfx.core.modelo;

import java.time.LocalDateTime;
import java.util.List;

/** Toma de inventario (conteo fisico). */
public record TomaInventario(
        Long id,
        long empresaId,
        long bodegaId,
        String bodega,
        String numero,
        String estado,
        boolean conteoCiego,
        LocalDateTime fechaInicio,
        LocalDateTime fechaCierre,
        String observacion,
        List<TomaDetalle> detalles) {
}
