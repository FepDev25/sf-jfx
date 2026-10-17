package ec.sfjfx.core.reporte;

import java.time.LocalDate;

/** Configuracion de filtros guardada por un usuario. */
public record ReporteGuardado(Long id, long empresaId, Long usuarioId, String codigo,
                              String nombre, LocalDate desde, LocalDate hasta,
                              Long sucursalId, Long categoriaId, Long filtroUsuarioId) {

    public FiltroReporte filtro() {
        return new FiltroReporte(desde, hasta, sucursalId, categoriaId, filtroUsuarioId);
    }

    @Override
    public String toString() {
        return nombre + " (" + codigo + ")";
    }
}
