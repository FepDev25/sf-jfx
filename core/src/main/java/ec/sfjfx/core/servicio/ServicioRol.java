package ec.sfjfx.core.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ec.sfjfx.core.modelo.Rol;
import ec.sfjfx.core.repositorio.RolRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioRol {

    private final RolRepositorio repo = new RolRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Rol> listar() {
        return repo.listar(empresaActual());
    }

    public Set<String> permisosDe(long rolId) {
        return repo.permisosDe(rolId);
    }

    public Rol guardar(Rol rol, Set<String> permisos) {
        Validaciones.requerido(rol.nombre(), "Nombre");
        if (repo.nombreExiste(rol.nombre(), empresaActual(), rol.id())) {
            throw new ValidacionExcepcion("Ya existe un rol con ese nombre");
        }
        if (rol.id() == null) {
            long id = repo.crear(new Rol(null, empresaActual(), rol.nombre(), rol.descripcion(), false, rol.activo()));
            repo.asignarPermisos(id, permisos);
            auditoria.registrar("ROLES", "rol", id, "CREAR", null, Map.of("nombre", rol.nombre(),
                    "permisos", permisos.size()));
            return new Rol(id, empresaActual(), rol.nombre(), rol.descripcion(), false, rol.activo());
        }
        repo.actualizar(rol);
        repo.asignarPermisos(rol.id(), permisos);
        auditoria.registrar("ROLES", "rol", rol.id(), "EDITAR", null, Map.of("nombre", rol.nombre(),
                "permisos", permisos.size()));
        return rol;
    }

    public void eliminar(Rol rol) {
        if (rol.esSistema()) {
            throw new ValidacionExcepcion("No se puede eliminar un rol del sistema");
        }
        if (repo.contarUsuariosConRol(rol.id()) > 0) {
            throw new ValidacionExcepcion("El rol esta asignado a usuarios; reasigne antes de eliminar");
        }
        repo.eliminar(rol.id());
        Map<String, Object> anterior = new LinkedHashMap<>();
        anterior.put("nombre", rol.nombre());
        auditoria.registrar("ROLES", "rol", rol.id(), "ELIMINAR", anterior, null);
    }

    private long empresaActual() {
        return SesionActual.requerida().empresa().id();
    }
}
