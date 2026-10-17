package ec.sfjfx.core.repositorio;

/**
 * Datos internos de autenticacion de un usuario (incluye hashes).
 * No debe exponerse a la UI.
 */
public record DatosAutenticacion(
        long id,
        Long empresaId,
        String nombreUsuario,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        String passwordHash,
        String pinHash,
        boolean esSuperadmin,
        boolean activo,
        boolean bloqueado,
        int intentosFallidos) {
}
