package ec.sfjfx.core.modelo;

/** Contacto de un cliente. */
public record ClienteContacto(
        Long id,
        long clienteId,
        String nombre,
        String telefono,
        String email,
        String cargo) {
}
