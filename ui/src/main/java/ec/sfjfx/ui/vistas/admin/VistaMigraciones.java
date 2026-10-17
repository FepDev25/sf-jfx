package ec.sfjfx.ui.vistas.admin;

import java.time.format.DateTimeFormatter;

import ec.sfjfx.core.db.MigracionInfo;
import ec.sfjfx.core.servicio.ServicioMigraciones;
import ec.sfjfx.ui.componentes.VistaLista;
import ec.sfjfx.ui.util.Alertas;
import ec.sfjfx.ui.util.Perm;

public final class VistaMigraciones extends VistaLista<MigracionInfo> {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ServicioMigraciones servicio = new ServicioMigraciones();

    public VistaMigraciones() {
        super("Migraciones de esquema");
        columna("Version", 90, MigracionInfo::version);
        columna("Descripcion", 320, MigracionInfo::descripcion);
        columna("Estado", 110, MigracionInfo::estado);
        columna("Aplicada en", 150, m -> m.aplicadaEn() == null ? "-" : m.aplicadaEn().format(FECHA));
        buscarPor(m -> m.version() + " " + m.descripcion() + " " + m.estado());
        proveedor(servicio::estado);

        if (Perm.tiene("configuracion.editar")) {
            accion("Aplicar pendientes", true, v -> aplicar());
        }
        refrescar();
    }

    private void aplicar() {
        long pendientes = servicio.estado().stream().filter(m -> !m.aplicada()).count();
        if (pendientes == 0) {
            Alertas.info("Migraciones", "No hay migraciones pendientes.");
            return;
        }
        if (!Alertas.confirmar("Aplicar migraciones",
                "Se aplicaran " + pendientes + " migracion(es) pendientes sobre la base de datos.\n"
                + "¿Desea continuar?")) {
            return;
        }
        try {
            int aplicadas = servicio.aplicarPendientes();
            refrescar();
            Alertas.info("Migraciones", "Migraciones aplicadas: " + aplicadas);
        } catch (RuntimeException e) {
            Alertas.error("No se pudieron aplicar las migraciones", e.getMessage());
        }
    }
}
