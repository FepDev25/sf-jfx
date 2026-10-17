package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Orden de compra. */
public record OrdenCompra(
        Long id,
        long empresaId,
        long sucursalId,
        long proveedorId,
        String proveedorNombre,
        String numero,
        String estado,
        LocalDateTime fechaEmision,
        LocalDate fechaEntregaEsperada,
        String condicionesPago,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal impuestos,
        BigDecimal total,
        String nota,
        List<OrdenCompraDetalle> detalles) {
}
