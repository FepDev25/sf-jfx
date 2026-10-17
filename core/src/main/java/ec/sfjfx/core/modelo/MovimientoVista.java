package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del kardex (movimiento de inventario) para mostrar. */
public record MovimientoVista(
        long id,
        LocalDateTime fecha,
        String bodega,
        String producto,
        String tipo,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal saldoCantidad,
        String referencia,
        String usuario,
        String nota) {
}
