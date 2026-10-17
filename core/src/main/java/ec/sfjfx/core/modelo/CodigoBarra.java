package ec.sfjfx.core.modelo;

/** Codigo de barras de un producto. */
public record CodigoBarra(
        Long id,
        long productoId,
        String codigo,
        TipoCodigoBarra tipo,
        boolean esPrincipal,
        Long empaqueId) {

    @Override
    public String toString() {
        return codigo;
    }
}
