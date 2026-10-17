package ec.sfjfx.core.modelo;

/** Usuario del sistema (sin datos sensibles como hashes). */
public record Usuario(
        Long id,
        Long empresaId,
        String nombreUsuario,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        boolean esSuperadmin,
        boolean activo) {

    public String nombreCompleto() {
        String n = nombres == null ? "" : nombres;
        String a = apellidos == null ? "" : apellidos;
        String completo = (n + " " + a).trim();
        return completo.isBlank() ? nombreUsuario : completo;
    }
}
