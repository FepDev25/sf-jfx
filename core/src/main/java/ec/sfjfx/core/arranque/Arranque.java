package ec.sfjfx.core.arranque;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.db.MigradorEsquema;

/**
 * Arranque de base de datos: aplica las migraciones pendientes.
 *
 * <p><strong>No ejecuta semillas.</strong> Las semillas son herramientas del
 * modulo {@code dev} y solo se ejecutan manualmente en un entorno local:
 * {@code mvn -pl dev exec:java}.
 *
 * <p>Uso: {@code mvn -pl core exec:java}
 */
public final class Arranque {

    private Arranque() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig.cargar();

        try (Connection conexion = ConexionBD.abrir()) {
            System.out.println("Entorno: " + AppConfig.entorno());
            System.out.println("Conectado a: " + conexion.getMetaData().getURL());
            System.out.println("PostgreSQL:  " + conexion.getMetaData().getDatabaseProductVersion());

            List<String> historial = ejecutarMigraciones(conexion);
            System.out.println("Migraciones aplicadas (historial): " + historial);
        }

        System.out.println("Semillas: no se ejecutan desde Arranque.");
        System.out.println("Para datos de desarrollo/demostracion use el modulo dev:");
        System.out.println("  mvn -pl dev exec:java");
        System.out.println("Listo.");
    }

    /**
     * Aplica las migraciones pendientes sobre la conexion indicada y devuelve el historial.
     *
     * <p>Metodo separado de {@link #main(String[])} para poder probar el arranque sin semillas.
     */
    public static List<String> ejecutarMigraciones(Connection conexion) throws SQLException, IOException {
        MigradorEsquema migrador = new MigradorEsquema(conexion);
        migrador.migrar();
        return migrador.historial();
    }
}
