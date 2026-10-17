package ec.sfjfx.core.seguridad;

/** Error de autenticacion (credenciales, usuario inactivo o bloqueado). */
public class AutenticacionExcepcion extends Exception {

    public AutenticacionExcepcion(String mensaje) {
        super(mensaje);
    }
}
