package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.db.MigracionInfo;
import ec.sfjfx.core.db.MigradorEsquema;
import ec.sfjfx.core.util.ValidacionExcepcion;

/** Consulta y aplicacion de migraciones de esquema desde la UI. */
public final class ServicioMigraciones {

    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public List<MigracionInfo> estado() {
        return Bd.consulta(c -> {
            try {
                return new MigradorEsquema(c).estado();
            } catch (IOException e) {
                throw new SQLException("No se pudo leer una migracion: " + e.getMessage(), e);
            }
        });
    }

    public int aplicarPendientes() {
        long pendientes = estado().stream().filter(m -> !m.aplicada()).count();
        try {
            Bd.consulta(c -> {
                try {
                    new MigradorEsquema(c).migrar();
                    return null;
                } catch (IOException e) {
                    throw new SQLException("No se pudo leer una migracion: " + e.getMessage(), e);
                }
            });
        } catch (RuntimeException e) {
            throw new ValidacionExcepcion("No se pudieron aplicar las migraciones: " + e.getMessage());
        }
        if (pendientes > 0) {
            auditoria.registrar("CONFIGURACION", "migracion", null, "EDITAR", null,
                    Map.of("aplicadas", pendientes));
        }
        return (int) pendientes;
    }
}
