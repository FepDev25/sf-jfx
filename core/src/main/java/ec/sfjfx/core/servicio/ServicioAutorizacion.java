package ec.sfjfx.core.servicio;

import java.util.Map;

import ec.sfjfx.core.repositorio.DatosAutenticacion;
import ec.sfjfx.core.repositorio.PermisoRepositorio;
import ec.sfjfx.core.repositorio.UsuarioRepositorio;
import ec.sfjfx.core.seguridad.SeguridadPass;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Autorizacion rapida de acciones sensibles mediante usuario + PIN. */
public final class ServicioAutorizacion {

    public record Autorizacion(long usuarioId, String nombre) {
    }

    private final UsuarioRepositorio usuarios = new UsuarioRepositorio();
    private final PermisoRepositorio permisos = new PermisoRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public Autorizacion autorizar(String nombreUsuario, String pin, String permiso) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || pin == null || pin.isBlank()) {
            throw new ValidacionExcepcion("Ingrese usuario y PIN de autorizacion");
        }
        DatosAutenticacion datos = usuarios.porNombreUsuario(nombreUsuario.trim())
                .orElseThrow(() -> new ValidacionExcepcion("Usuario o PIN incorrectos"));
        if (!datos.activo() || datos.bloqueado() || datos.pinHash() == null
                || !SeguridadPass.verificar(pin, datos.pinHash())) {
            throw new ValidacionExcepcion("Usuario o PIN incorrectos");
        }
        boolean permitido = datos.esSuperadmin() || permisos.codigosPorUsuario(datos.id()).contains(permiso);
        if (!permitido) {
            throw new ValidacionExcepcion("El usuario no tiene el permiso requerido: " + permiso);
        }
        String nombre = (texto(datos.nombres()) + " " + texto(datos.apellidos())).trim();
        if (nombre.isBlank()) {
            nombre = datos.nombreUsuario();
        }
        auditoria.registrar("POS", "autorizacion", datos.id(), "AUTORIZAR", null, Map.of("permiso", permiso));
        return new Autorizacion(datos.id(), nombre);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
