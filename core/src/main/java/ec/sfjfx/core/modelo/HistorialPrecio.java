package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del historial de precios/costos. */
public record HistorialPrecio(
        long id,
        long productoId,
        Long listaPrecioId,
        String tipo,
        BigDecimal valorAnterior,
        BigDecimal valorNuevo,
        LocalDateTime creadoEn,
        String usuarioNombre) {
}
