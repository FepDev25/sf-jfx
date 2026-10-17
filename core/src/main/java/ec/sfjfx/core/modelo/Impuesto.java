package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Impuesto (IVA, ICE, IRBPNR...) con vigencia. */
public record Impuesto(
        Long id,
        Long empresaId,
        String codigoSri,
        String nombre,
        TipoImpuesto tipo,
        BigDecimal porcentaje,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean esRetencion,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
