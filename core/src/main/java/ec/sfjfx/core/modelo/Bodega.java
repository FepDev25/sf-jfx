package ec.sfjfx.core.modelo;

/** Bodega / almacen dentro de una sucursal. */
public record Bodega(
        Long id,
        long sucursalId,
        String codigo,
        String nombre,
        String tipo,
        String direccion,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
