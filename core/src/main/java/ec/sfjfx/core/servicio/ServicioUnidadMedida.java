package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.UnidadMedida;
import ec.sfjfx.core.repositorio.UnidadMedidaRepositorio;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioUnidadMedida {

    private final UnidadMedidaRepositorio repo = new UnidadMedidaRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<UnidadMedida> listar() {
        return repo.listar();
    }

    public UnidadMedida guardar(UnidadMedida u) {
        Validaciones.requerido(u.codigo(), "Codigo");
        Validaciones.requerido(u.nombre(), "Nombre");
        if (u.id() == null) {
            long id = repo.crear(u);
            auditoria.registrar("PRODUCTOS", "unidad_medida", id, "CREAR", null, Map.of("codigo", u.codigo()));
            return new UnidadMedida(id, u.codigo(), u.nombre(), u.abreviatura(), u.tipo(),
                    u.permiteDecimales(), u.activo());
        }
        repo.actualizar(u);
        auditoria.registrar("PRODUCTOS", "unidad_medida", u.id(), "EDITAR", null, Map.of("codigo", u.codigo()));
        return u;
    }
}
