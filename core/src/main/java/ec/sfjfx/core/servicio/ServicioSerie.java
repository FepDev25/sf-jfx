package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Serie;
import ec.sfjfx.core.repositorio.SerieRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioSerie {

    private final SerieRepositorio repo = new SerieRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Serie> listar(String filtro) {
        return repo.listar(SesionActual.requerida().empresa().id(), filtro);
    }

    public List<Serie> porProducto(long productoId) {
        return repo.porProducto(productoId);
    }

    public long registrar(long productoId, String numeroSerie, Long bodegaId, Long loteId) {
        Validaciones.requerido(numeroSerie, "Numero de serie");
        long id = Bd.transaccion(c -> repo.buscarOCrear(c, productoId, numeroSerie.trim(), bodegaId, loteId));
        auditoria.registrar("INVENTARIO", "serie", id, "CREAR", null, Map.of("numero", numeroSerie));
        return id;
    }

    public void cambiarEstado(long serieId, String estado) {
        repo.cambiarEstado(serieId, estado);
        auditoria.registrar("INVENTARIO", "serie", serieId, "EDITAR", null, Map.of("estado", estado));
    }
}
