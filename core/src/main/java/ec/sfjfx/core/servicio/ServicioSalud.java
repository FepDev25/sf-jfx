package ec.sfjfx.core.servicio;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.config.AppInfo;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.db.MigracionInfo;
import ec.sfjfx.core.db.MigradorEsquema;
import ec.sfjfx.core.modelo.Instalacion;
import ec.sfjfx.core.repositorio.InstalacionRepositorio;

/**
 * Comprobacion de salud de la instalacion.
 *
 * <p>La usan la pantalla de estado, la CLI ({@code Arranque salud}) y cualquier monitorizacion
 * externa. Devuelve un codigo de salida utilizable en tareas del sistema operativo.
 */
public final class ServicioSalud {

    /** Resultado de la comprobacion. */
    public record Estado(
            boolean correcto,
            String version,
            String entorno,
            String baseDatos,
            String esquema,
            int migracionesPendientes,
            String instalacion,
            String licencia,
            boolean modoConsulta,
            String mensaje) {
    }

    public Estado comprobar() {
        String version = AppInfo.version();
        String entorno = AppConfig.entorno();
        String base = "desconocida";
        String esquema = "desconocida";
        int pendientes = 0;
        boolean correcto = true;
        StringBuilder mensajes = new StringBuilder();

        try (Connection conexion = ConexionBD.abrir()) {
            base = conexion.getMetaData().getDatabaseProductVersion();
            MigradorEsquema migrador = new MigradorEsquema(conexion);
            List<MigracionInfo> estado = migrador.estado();
            pendientes = (int) estado.stream().filter(m -> !m.aplicada()).count();
            List<String> historial = migrador.historial();
            esquema = historial.isEmpty() ? "sin migraciones" : String.join(", ", historial);
            if (pendientes > 0) {
                correcto = false;
                mensajes.append(pendientes).append(" migracion(es) pendiente(s). ");
            }
        } catch (SQLException | IOException | RuntimeException e) {
            correcto = false;
            mensajes.append("No se pudo consultar la base: ").append(detalle(e)).append(". ");
        }

        String nombreInstalacion = "no registrada";
        try {
            Instalacion instalacion = new InstalacionRepositorio().obtener().orElse(null);
            if (instalacion == null) {
                correcto = false;
                mensajes.append("La instalacion no esta registrada. ");
            } else {
                nombreInstalacion = instalacion.nombre() + " (" + instalacion.modo() + ")";
            }
        } catch (RuntimeException e) {
            correcto = false;
            mensajes.append("No se pudo leer la instalacion: ").append(detalle(e)).append(". ");
        }

        String licencia = "desconocida";
        boolean modoConsulta = false;
        try {
            ServicioLicencia.EstadoLicencia estadoLicencia = new ServicioLicencia().estado();
            licencia = estadoLicencia.valida() ? "valida" : "no valida";
            modoConsulta = estadoLicencia.modoConsulta();
            if (modoConsulta) {
                mensajes.append("Modo consulta: ").append(estadoLicencia.motivo()).append(" ");
            }
        } catch (RuntimeException e) {
            licencia = "ERROR " + e.getMessage();
        }

        String mensaje = mensajes.length() == 0 ? "Instalacion correcta" : mensajes.toString().trim();
        return new Estado(correcto, version, entorno, base, esquema, pendientes,
                nombreInstalacion, licencia, modoConsulta, mensaje);
    }

    /** Detalle legible de una excepcion, incluyendo la causa original. */
    private static String detalle(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String mensaje = causa.getMessage();
        return mensaje == null || mensaje.isBlank() ? causa.getClass().getSimpleName() : mensaje;
    }
}
