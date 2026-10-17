package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de solicitud de compra. */
public record SolicitudDetalle(
        Long id,
        long solicitudId,
        long productoId,
        String productoNombre,
        BigDecimal cantidad,
        String justificacion) {
}
