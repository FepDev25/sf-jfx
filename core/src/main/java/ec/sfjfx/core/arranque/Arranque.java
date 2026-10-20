package ec.sfjfx.core.arranque;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.db.MigradorEsquema;
import ec.sfjfx.core.servicio.ServicioLicencia;
import ec.sfjfx.core.servicio.ServicioLog;
import ec.sfjfx.core.servicio.ServicioInstalacion;
import ec.sfjfx.core.servicio.ServicioRespaldo;
import ec.sfjfx.core.servicio.ServicioSalud;

/**
 * Arranque de base de datos: aplica migraciones pendientes.
 *
 * <p><strong>No ejecuta semillas.</strong> Las semillas son herramientas del
 * modulo {@code dev} y solo se ejecutan manualmente en un entorno local:
 * {@code mvn -pl dev exec:java}.
 *
 * <p>Modos de uso:
 * <ul>
 *   <li>{@code mvn -pl core exec:java} — aplica migraciones pendientes.</li>
 *   <li>{@code mvn -pl core exec:java -Dexec.args="respaldo"} — genera y verifica un respaldo.
 *       Pensado para ser invocado por una tarea del sistema operativo (cron/Task Scheduler).</li>
 *   <li>{@code mvn -pl core exec:java -Dexec.args="salud"} — comprueba la instalacion y devuelve
 *       codigo de salida 0 (ok) o 1 (con problemas). Pensado para monitorizacion.</li>
 * </ul>
 */
public final class Arranque {

    private Arranque() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig.cargar();
        new ServicioLog().redirigirSalida();
        String modo = args.length > 0 ? args[0].trim().toLowerCase(java.util.Locale.ROOT) : "";

        switch (modo) {
            case "respaldo" -> {
                int codigo = respaldo();
                if (codigo != 0) {
                    System.exit(codigo);
                }
            }
            case "salud" -> {
                int codigo = salud();
                System.exit(codigo);
            }
            default -> migrar();
        }
    }

    private static void migrar() throws Exception {
        try (Connection conexion = ConexionBD.abrir()) {
            System.out.println("Entorno: " + AppConfig.entorno());
            System.out.println("Conectado a: " + conexion.getMetaData().getURL());
            System.out.println("PostgreSQL:  " + conexion.getMetaData().getDatabaseProductVersion());

            List<String> historial = ejecutarMigraciones(conexion);
            System.out.println("Migraciones aplicadas (historial): " + historial);
        }

        // Registra los metadatos de instalacion si la base ya tenia empresas.
        new ServicioInstalacion().registrarSiFalta();

        System.out.println("Semillas: no se ejecutan desde Arranque.");
        System.out.println("Para datos de desarrollo/demostracion use el modulo dev:");
        System.out.println("  mvn -pl dev exec:java");
        System.out.println("Listo.");
    }

    /**
     * Genera un respaldo verificado. Pensado para tareas del sistema operativo, por eso no
     * requiere sesion de usuario.
     */
    private static int respaldo() {
        try {
            ServicioRespaldo.RespaldoResultado resultado =
                    new ServicioRespaldo().respaldarDesdeConsola();            System.out.println("Respaldo generado: " + resultado.ruta());
            System.out.println("Tamano: " + resultado.tamanoLegible());
            System.out.println("Verificado: " + resultado.verificado());
            return 0;
        } catch (RuntimeException e) {
            System.err.println("No se pudo generar el respaldo: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Comprueba la instalacion: base conectada, esquema al dia, instalacion registrada y licencia.
     *
     * @return 0 si todo esta correcto, 1 si hay problemas.
     */
    private static int salud() {
        ServicioSalud.Estado estado = new ServicioSalud().comprobar();
        System.out.println("version: " + estado.version());
        System.out.println("entorno: " + estado.entorno());
        System.out.println("db: " + estado.baseDatos());
        System.out.println("esquema: " + estado.esquema()
                + (estado.migracionesPendientes() > 0
                        ? " · " + estado.migracionesPendientes() + " pendiente(s)" : ""));
        System.out.println("instalacion: " + estado.instalacion());
        System.out.println("licencia: " + estado.licencia()
                + (estado.modoConsulta() ? " · MODO CONSULTA" : ""));
        System.out.println("salud: " + (estado.correcto() ? "OK" : "CON PROBLEMAS")
                + (estado.mensaje() == null || estado.mensaje().isBlank() ? "" : " · " + estado.mensaje()));
        return estado.correcto() ? 0 : 1;
    }

    /**
     * Aplica las migraciones pendientes sobre la conexion indicada y devuelve el historial.
     *
     * <p>Metodo separado de {@link #main(String[])} para poder probar el arranque sin semillas.
     */
    public static List<String> ejecutarMigraciones(Connection conexion) throws SQLException, java.io.IOException {
        MigradorEsquema migrador = new MigradorEsquema(conexion);
        migrador.migrar();
        return migrador.historial();
    }
}
