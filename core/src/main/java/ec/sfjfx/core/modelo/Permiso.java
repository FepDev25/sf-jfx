package ec.sfjfx.core.modelo;

/** Permiso (modulo + accion). */
public record Permiso(
        long id,
        String codigo,
        String modulo,
        String accion,
        String descripcion) {
}
