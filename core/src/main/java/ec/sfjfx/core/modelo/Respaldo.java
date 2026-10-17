package ec.sfjfx.core.modelo;

import java.time.LocalDateTime;

/** Registro de un respaldo de la base de datos. */
public record Respaldo(
        Long id,
        Long empresaId,
        String rutaArchivo,
        long tamanoBytes,
        String tipo,
        String estado,
        Long usuarioId,
        LocalDateTime creadoEn,
        String detalle) {

    public boolean correcto() {
        return "OK".equals(estado);
    }

    public String tamanoLegible() {
        if (tamanoBytes < 1024) {
            return tamanoBytes + " B";
        }
        double kb = tamanoBytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        return String.format("%.1f MB", kb / 1024.0);
    }
}
