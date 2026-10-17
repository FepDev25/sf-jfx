package ec.sfjfx.core.db;

import java.time.LocalDateTime;

/** Estado de una migracion de esquema (aplicada o pendiente). */
public record MigracionInfo(String version, String descripcion, boolean aplicada,
                            LocalDateTime aplicadaEn) {

    public String estado() {
        return aplicada ? "APLICADA" : "PENDIENTE";
    }
}
