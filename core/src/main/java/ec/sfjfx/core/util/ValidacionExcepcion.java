package ec.sfjfx.core.util;

/** Error de validacion de datos de entrada. */
public class ValidacionExcepcion extends RuntimeException {

    public ValidacionExcepcion(String mensaje) {
        super(mensaje);
    }
}
