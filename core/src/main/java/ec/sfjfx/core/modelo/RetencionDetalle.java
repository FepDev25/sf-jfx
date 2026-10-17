package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Linea de retencion (IVA o Renta). */
public record RetencionDetalle(
        Long id,
        long retencionId,
        String tipoImpuesto,
        String codigoRetencion,
        BigDecimal baseImponible,
        BigDecimal porcentaje,
        BigDecimal valorRetenido) {
}
