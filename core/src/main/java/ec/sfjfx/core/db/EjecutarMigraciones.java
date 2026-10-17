package ec.sfjfx.core.db;

import ec.sfjfx.core.config.AppConfig;

import java.sql.Connection;

/**
 * Ejecutor de migraciones por linea de comandos.
 *
 * <p>Uso: {@code mvn -pl core exec:java}
 */
public final class EjecutarMigraciones {

    private EjecutarMigraciones() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig.cargar();
        try (Connection conexion = ConexionBD.abrir()) {
            System.out.println("Conectado a: " + conexion.getMetaData().getURL());
            System.out.println("PostgreSQL:  " + conexion.getMetaData().getDatabaseProductVersion());

            MigradorEsquema migrador = new MigradorEsquema(conexion);
            migrador.migrar();

            System.out.println("Migraciones aplicadas (historial): " + migrador.historial());
            System.out.println("Listo.");
        }
    }
}
