package ec.sfjfx.core.repositorio;

/** Error de acceso a datos. */
public class RepositorioExcepcion extends RuntimeException {

    public RepositorioExcepcion(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
