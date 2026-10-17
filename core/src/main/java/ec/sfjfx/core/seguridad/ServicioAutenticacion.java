package ec.sfjfx.core.seguridad;

import java.util.Optional;
import java.util.Set;

import ec.sfjfx.core.modelo.Empresa;
import ec.sfjfx.core.modelo.Usuario;
import ec.sfjfx.core.repositorio.DatosAutenticacion;
import ec.sfjfx.core.repositorio.EmpresaRepositorio;
import ec.sfjfx.core.repositorio.PermisoRepositorio;
import ec.sfjfx.core.repositorio.UsuarioRepositorio;

/** Autentica usuarios y construye la {@link Sesion}. */
public final class ServicioAutenticacion {

    private static final int MAX_INTENTOS = 5;

    private final UsuarioRepositorio usuarios;
    private final PermisoRepositorio permisos;
    private final EmpresaRepositorio empresas;

    public ServicioAutenticacion(UsuarioRepositorio usuarios,
                                 PermisoRepositorio permisos,
                                 EmpresaRepositorio empresas) {
        this.usuarios = usuarios;
        this.permisos = permisos;
        this.empresas = empresas;
    }

    public ServicioAutenticacion() {
        this(new UsuarioRepositorio(), new PermisoRepositorio(), new EmpresaRepositorio());
    }

    public Sesion iniciar(String nombreUsuario, String password) throws AutenticacionExcepcion {
        if (nombreUsuario == null || nombreUsuario.isBlank() || password == null) {
            throw new AutenticacionExcepcion("Ingrese usuario y contrasena");
        }
        DatosAutenticacion datos = usuarios.porNombreUsuario(nombreUsuario.trim())
                .orElseThrow(() -> new AutenticacionExcepcion("Usuario o contrasena incorrectos"));

        if (!datos.activo()) {
            throw new AutenticacionExcepcion("El usuario esta inactivo");
        }
        if (datos.bloqueado()) {
            throw new AutenticacionExcepcion("El usuario esta bloqueado. Contacte al administrador");
        }
        if (!SeguridadPass.verificar(password, datos.passwordHash())) {
            int intentos = usuarios.registrarIntentoFallido(datos.id());
            if (intentos >= MAX_INTENTOS) {
                usuarios.bloquear(datos.id());
                throw new AutenticacionExcepcion("Usuario bloqueado por demasiados intentos fallidos");
            }
            int restantes = MAX_INTENTOS - intentos;
            throw new AutenticacionExcepcion("Usuario o contrasena incorrectos. Intentos restantes: " + restantes);
        }

        usuarios.marcarAcceso(datos.id());

        Empresa empresa = resolverEmpresa(datos);
        Set<String> codigos = datos.esSuperadmin()
                ? permisos.todosLosCodigos()
                : permisos.codigosPorUsuario(datos.id());

        Usuario usuario = new Usuario(
                datos.id(), datos.empresaId(), datos.nombreUsuario(),
                datos.nombres(), datos.apellidos(), datos.email(), datos.telefono(),
                datos.esSuperadmin(), datos.activo());

        return new Sesion(usuario, empresa, codigos);
    }

    private Empresa resolverEmpresa(DatosAutenticacion datos) throws AutenticacionExcepcion {
        Optional<Empresa> empresa = datos.empresaId() != null
                ? empresas.porId(datos.empresaId())
                : empresas.primeraActiva();
        return empresa.orElseThrow(() -> new AutenticacionExcepcion(
                "No hay empresa configurada. Ejecute la semilla inicial"));
    }
}
