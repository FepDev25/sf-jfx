package ec.sfjfx.core.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Bodega;
import ec.sfjfx.core.repositorio.BodegaRepositorio;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioBodega {

    private final BodegaRepositorio repo = new BodegaRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Bodega> listarPorSucursal(long sucursalId) {
        return repo.porSucursal(sucursalId);
    }

    public Bodega guardar(Bodega b) {
        Validaciones.requerido(b.codigo(), "Codigo");
        Validaciones.requerido(b.nombre(), "Nombre");
        Validaciones.requerido(b.tipo(), "Tipo");
        if (b.id() == null) {
            long id = repo.crear(b);
            auditoria.registrar("BODEGAS", "bodega", id, "CREAR", null, datos(b));
            return new Bodega(id, b.sucursalId(), b.codigo(), b.nombre(), b.tipo(), b.direccion(), b.activo());
        }
        repo.actualizar(b);
        auditoria.registrar("BODEGAS", "bodega", b.id(), "EDITAR", null, datos(b));
        return b;
    }

    private Map<String, Object> datos(Bodega b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", b.codigo());
        m.put("nombre", b.nombre());
        m.put("tipo", b.tipo());
        m.put("activo", b.activo());
        return m;
    }
}
