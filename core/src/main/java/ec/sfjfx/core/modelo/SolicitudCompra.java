package ec.sfjfx.core.modelo;

import java.time.LocalDateTime;
import java.util.List;

/** Solicitud de compra (opcional dentro del ciclo). */
public record SolicitudCompra(
        Long id,
        long empresaId,
        long sucursalId,
        String numero,
        String estado,
        LocalDateTime fecha,
        String nota,
        List<SolicitudDetalle> detalles) {
}
