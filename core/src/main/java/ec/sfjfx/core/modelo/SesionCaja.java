package ec.sfjfx.core.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Sesion (turno) de caja. */
public record SesionCaja(
        Long id,
        long empresaId,
        long cajaId,
        String caja,
        Long usuarioAperturaId,
        Long usuarioCierreId,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        BigDecimal montoInicial,
        BigDecimal montoEsperado,
        BigDecimal montoContado,
        BigDecimal diferencia,
        String estado,
        String observacion) {

    public boolean abierta() {
        return "ABIERTA".equals(estado);
    }
}
