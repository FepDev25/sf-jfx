package ec.sfjfx.core.reporte;

import java.time.LocalDate;

/** Filtros parametrizables de los reportes. Un valor {@code null} significa "todos". */
public record FiltroReporte(LocalDate desde, LocalDate hasta, Long sucursalId,
                            Long categoriaId, Long usuarioId) {

    public static FiltroReporte ultimosDias(int dias) {
        LocalDate hasta = LocalDate.now();
        return new FiltroReporte(hasta.minusDays(dias - 1L), hasta, null, null, null);
    }

    public static FiltroReporte sinFiltros() {
        return new FiltroReporte(null, null, null, null, null);
    }

    public FiltroReporte conSucursal(Long id) {
        return new FiltroReporte(desde, hasta, id, categoriaId, usuarioId);
    }

    public FiltroReporte conCategoria(Long id) {
        return new FiltroReporte(desde, hasta, sucursalId, id, usuarioId);
    }

    public FiltroReporte conUsuario(Long id) {
        return new FiltroReporte(desde, hasta, sucursalId, categoriaId, id);
    }

    public String descripcion() {
        StringBuilder sb = new StringBuilder();
        sb.append(desde == null ? "Sin fecha inicial" : "Desde " + desde);
        sb.append(" · ");
        sb.append(hasta == null ? "Sin fecha final" : "Hasta " + hasta);
        return sb.toString();
    }
}
