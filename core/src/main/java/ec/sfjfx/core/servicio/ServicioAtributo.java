package ec.sfjfx.core.servicio;

import java.util.List;
import java.util.Map;

import ec.sfjfx.core.modelo.AtributoDefinicion;
import ec.sfjfx.core.repositorio.AtributoRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioAtributo {

    private final AtributoRepositorio repo = new AtributoRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<AtributoDefinicion> listar() {
        return repo.listar(empresa());
    }

    public List<AtributoDefinicion> aplicables(Long categoriaId) {
        return repo.aplicables(empresa(), categoriaId);
    }

    public AtributoDefinicion guardar(AtributoDefinicion a) {
        Validaciones.requerido(a.codigo(), "Codigo");
        Validaciones.requerido(a.nombre(), "Nombre");
        if (repo.codigoExiste(empresa(), a.codigo(), a.id())) {
            throw new ValidacionExcepcion("Ya existe un atributo con ese codigo");
        }
        if (a.id() == null) {
            long id = repo.crear(a);
            auditoria.registrar("PRODUCTOS", "atributo_definicion", id, "CREAR", null, Map.of("codigo", a.codigo()));
            return new AtributoDefinicion(id, a.empresaId(), a.categoriaId(), a.codigo(), a.nombre(),
                    a.tipoDato(), a.opciones(), a.obligatorio(), a.orden(), a.activo());
        }
        repo.actualizar(a);
        auditoria.registrar("PRODUCTOS", "atributo_definicion", a.id(), "EDITAR", null, Map.of("codigo", a.codigo()));
        return a;
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
