package ec.sfjfx.core.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.Caja;
import ec.sfjfx.core.repositorio.CajaRepositorio;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioCaja {

    private final CajaRepositorio repo = new CajaRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Caja> listarPorSucursal(long sucursalId) {
        return repo.porSucursal(sucursalId);
    }

    public Caja guardar(Caja k) {
        Validaciones.requerido(k.codigo(), "Codigo");
        Validaciones.requerido(k.nombre(), "Nombre");
        if (k.id() == null) {
            long id = repo.crear(k);
            auditoria.registrar("CAJAS", "caja", id, "CREAR", null, datos(k));
            return new Caja(id, k.sucursalId(), k.codigo(), k.nombre(), k.estado(), k.activo());
        }
        repo.actualizar(k);
        auditoria.registrar("CAJAS", "caja", k.id(), "EDITAR", null, datos(k));
        return k;
    }

    private Map<String, Object> datos(Caja k) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", k.codigo());
        m.put("nombre", k.nombre());
        m.put("estado", k.estado());
        m.put("activo", k.activo());
        return m;
    }
}
