package ec.sfjfx.core.modelo;

/** Numero de serie individual de un producto (opcional segun flag controlaSerie). */
public record Serie(
        Long id,
        long productoId,
        String productoNombre,
        String numeroSerie,
        String estado,
        Long bodegaId,
        String bodega,
        Long loteId,
        Long ventaId) {

    @Override
    public String toString() {
        return numeroSerie;
    }
}
