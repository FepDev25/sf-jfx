package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linea de recepcion de compra. */
public record RecepcionDetalle(
        Long id,
        long recepcionId,
        Long ordenCompraDetalleId,
        long productoId,
        String productoNombre,
        Long loteId,
        String loteCodigo,
        LocalDate fechaVencimiento,
        BigDecimal cantidadRecibida,
        BigDecimal costoUnitario) {
}
