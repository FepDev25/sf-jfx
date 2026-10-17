package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Comprobante de retencion a proveedor. */
public record Retencion(
        Long id,
        long empresaId,
        long proveedorId,
        String proveedorNombre,
        Long facturaProveedorId,
        String numero,
        LocalDate fechaEmision,
        BigDecimal baseIva,
        BigDecimal valorIva,
        BigDecimal valorRetencionIva,
        BigDecimal baseRenta,
        BigDecimal valorRetencionRenta,
        BigDecimal totalRetenido,
        String estado,
        List<RetencionDetalle> detalles) {
}
