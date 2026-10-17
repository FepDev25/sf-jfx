package ec.sfjfx.core.seguridad;

import java.util.Optional;

/**
 * Contenedor de la sesion actual de la aplicacion (una por proceso de escritorio).
 */
public final class SesionActual {

    private static Sesion sesion;

    private SesionActual() {
    }

    public static void iniciar(Sesion nueva) {
        sesion = nueva;
    }

    public static Optional<Sesion> obtener() {
        return Optional.ofNullable(sesion);
    }

    public static Sesion requerida() {
        if (sesion == null) {
            throw new IllegalStateException("No hay sesion activa");
        }
        return sesion;
    }

    public static boolean haySesion() {
        return sesion != null;
    }

    public static void cerrar() {
        sesion = null;
    }
}
