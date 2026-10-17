package ec.sfjfx.core.modelo;

/** Valor de un atributo personalizado en un producto. */
public record ProductoAtributo(
        Long id,
        long productoId,
        long atributoDefinicionId,
        String valor) {
}
