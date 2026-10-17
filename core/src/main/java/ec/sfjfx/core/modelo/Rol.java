package ec.sfjfx.core.modelo;

/** Rol del sistema (agrupa permisos). */
public record Rol(
        Long id,
        Long empresaId,
        String nombre,
        String descripcion,
        boolean esSistema,
        boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
