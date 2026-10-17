package ec.sfjfx.core.modelo;

/** Lista de precios. */
public record ListaPrecio(
        Long id,
        long empresaId,
        String nombre,
        TipoListaPrecio tipo,
        String moneda,
        boolean incluyeIva,
        boolean esDefault,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
