package ec.sfjfx.core.modelo;

/** Caja / terminal de punto de venta. */
public record Caja(
        Long id,
        long sucursalId,
        String codigo,
        String nombre,
        String estado,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
