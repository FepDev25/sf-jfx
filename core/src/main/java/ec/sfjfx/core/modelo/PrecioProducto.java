package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Precio de un producto en una lista (con vigencia y override por sucursal). */
public record PrecioProducto(
        Long id,
        long listaPrecioId,
        String listaPrecioNombre,
        long productoId,
        BigDecimal precio,
        Long sucursalId,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta) {
}
