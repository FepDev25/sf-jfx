package ec.sfjfx.core.modelo;

/** Imagen de un producto. */
public record ImagenProducto(
        Long id,
        long productoId,
        String ruta,
        String nombreArchivo,
        int orden,
        boolean esPrincipal) {
}
