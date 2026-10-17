package ec.sfjfx.core.modelo;

import java.math.BigDecimal;

/** Solicitud interna de un movimiento de inventario (cantidad con signo). */
public record MovimientoSolicitud(
        long empresaId,
        long sucursalId,
        long bodegaId,
        long productoId,
        Long loteId,
        Long serieId,
        String tipoMovimiento,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        String referenciaTipo,
        Long referenciaId,
        Long usuarioId,
        String nota) {
}
