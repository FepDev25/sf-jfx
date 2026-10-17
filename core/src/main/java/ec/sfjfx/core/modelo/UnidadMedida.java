package ec.sfjfx.core.modelo;

/** Unidad de medida. */
public record UnidadMedida(
        Long id,
        String codigo,
        String nombre,
        String abreviatura,
        TipoUnidad tipo,
        boolean permiteDecimales,
        boolean activo) {

    @Override
    public String toString() {
        return nombre + " (" + abreviatura + ")";
    }
}
