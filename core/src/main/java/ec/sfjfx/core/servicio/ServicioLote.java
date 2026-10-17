package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Lote;
import ec.sfjfx.core.repositorio.LoteRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioLote {

    private final LoteRepositorio repo = new LoteRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Lote> listar(String filtro) {
        return repo.listar(SesionActual.requerida().empresa().id(), filtro);
    }

    public List<Lote> porProducto(long productoId) {
        return repo.porProducto(productoId);
    }

    public long crear(Lote lote) {
        Validaciones.requerido(lote.codigo(), "Codigo de lote");
        long id = repo.crear(lote);
        auditoria.registrar("INVENTARIO", "lote", id, "CREAR", null,
                Map.of("codigo", lote.codigo(), "producto", lote.productoId()));
        return id;
    }
}
