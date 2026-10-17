package ec.sfjfx.core.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ec.sfjfx.core.modelo.Rol;
import ec.sfjfx.core.modelo.Usuario;
import ec.sfjfx.core.repositorio.RolRepositorio;
import ec.sfjfx.core.repositorio.UsuarioRepositorio;
import ec.sfjfx.core.seguridad.SeguridadPass;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.core.util.ValidacionExcepcion;
import ec.sfjfx.core.util.Validaciones;

public final class ServicioUsuario {

    private final UsuarioRepositorio repo = new UsuarioRepositorio();
    private final RolRepositorio roles = new RolRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<Usuario> listar() {
        return repo.listar(empresaActual());
    }

    public List<Rol> rolesDisponibles() {
        return roles.listar(empresaActual());
    }

    public Usuario crear(Usuario u, String password) {
        validar(u, true);
        Validaciones.requerido(password, "Contrasena");
        if (password.length() < 4) {
            throw new ValidacionExcepcion("La contrasena debe tener al menos 4 caracteres");
        }
        long id = repo.crear(u, SeguridadPass.hashear(password), null);
        auditoria.registrar("USUARIOS", "usuario", id, "CREAR", null, datos(u));
        return new Usuario(id, u.empresaId(), u.nombreUsuario(), u.nombres(), u.apellidos(),
                u.email(), u.telefono(), u.esSuperadmin(), u.activo());
    }

    public void actualizar(Usuario u) {
        validar(u, false);
        repo.actualizar(u);
        auditoria.registrar("USUARIOS", "usuario", u.id(), "EDITAR", null, datos(u));
    }

    public void cambiarPassword(long usuarioId, String password) {
        Validaciones.requerido(password, "Contrasena");
        if (password.length() < 4) {
            throw new ValidacionExcepcion("La contrasena debe tener al menos 4 caracteres");
        }
        repo.cambiarPassword(usuarioId, SeguridadPass.hashear(password));
        auditoria.registrar("USUARIOS", "usuario", usuarioId, "EDITAR", null, Map.of("cambioClave", true));
    }

    public void cambiarPin(long usuarioId, String pin) {
        Validaciones.requerido(pin, "PIN");
        if (!pin.matches("\\d{4,8}")) {
            throw new ValidacionExcepcion("El PIN debe tener entre 4 y 8 digitos");
        }
        repo.cambiarPin(usuarioId, SeguridadPass.hashear(pin));
    }

    public void asignarRoles(long usuarioId, Set<Long> rolIds) {
        repo.asignarRoles(usuarioId, rolIds);
        auditoria.registrar("USUARIOS", "usuario", usuarioId, "EDITAR", null, Map.of("roles", rolIds));
    }

    public void asignarSucursales(long usuarioId, Set<Long> sucursalIds) {
        repo.asignarSucursales(usuarioId, sucursalIds);
        auditoria.registrar("USUARIOS", "usuario", usuarioId, "EDITAR", null, Map.of("sucursales", sucursalIds));
    }

    public void cambiarActivo(long usuarioId, boolean activo) {
        repo.cambiarActivo(usuarioId, activo);
        auditoria.registrar("USUARIOS", "usuario", usuarioId, activo ? "EDITAR" : "ELIMINAR",
                null, Map.of("activo", activo));
    }

    public List<Rol> rolesDe(long usuarioId) {
        return repo.rolesDe(usuarioId);
    }

    public Set<Long> sucursalesDe(long usuarioId) {
        return repo.sucursalesDe(usuarioId);
    }

    private long empresaActual() {
        return SesionActual.requerida().empresa().id();
    }

    private void validar(Usuario u, boolean esNuevo) {
        Validaciones.requerido(u.nombreUsuario(), "Usuario");
        Validaciones.requerido(u.nombres(), "Nombres");
        if (!Validaciones.email(u.email())) {
            throw new ValidacionExcepcion("El email no es valido");
        }
        if (repo.existeNombreUsuario(u.nombreUsuario(), esNuevo ? null : u.id())) {
            throw new ValidacionExcepcion("Ya existe un usuario con ese nombre");
        }
    }

    private Map<String, Object> datos(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("usuario", u.nombreUsuario());
        m.put("nombres", u.nombres());
        m.put("apellidos", u.apellidos());
        m.put("email", u.email());
        m.put("superadmin", u.esSuperadmin());
        m.put("activo", u.activo());
        return m;
    }
}
