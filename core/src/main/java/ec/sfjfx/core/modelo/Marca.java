package ec.sfjfx.core.modelo;

/** Marca / fabricante. */
public record Marca(
        Long id,
        long empresaId,
        String nombre,
        String descripcion,
        String logoPath,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
