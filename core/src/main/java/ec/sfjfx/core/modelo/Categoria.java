package ec.sfjfx.core.modelo;

/** Categoria (arbol jerarquico). */
public record Categoria(
        Long id,
        long empresaId,
        Long padreId,
        String codigo,
        String nombre,
        String descripcion,
        int orden,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
