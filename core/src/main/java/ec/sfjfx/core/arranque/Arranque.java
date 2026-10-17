package ec.sfjfx.core.arranque;

import java.sql.Connection;

import ec.sfjfx.core.config.AppConfig;
import ec.sfjfx.core.db.ConexionBD;
import ec.sfjfx.core.db.MigradorEsquema;

/**
 * Arranque de base de datos: aplica migraciones pendientes y luego la semilla
 * de desarrollo (idempotente).
 *
 * <p>Uso: {@code mvn -pl core exec:java}
 */
public final class Arranque {

    private Arranque() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig.cargar();

        try (Connection conexion = ConexionBD.abrir()) {
            System.out.println("Conectado a: " + conexion.getMetaData().getURL());
            System.out.println("PostgreSQL:  " + conexion.getMetaData().getDatabaseProductVersion());

            MigradorEsquema migrador = new MigradorEsquema(conexion);
            migrador.migrar();
            System.out.println("Migraciones aplicadas (historial): " + migrador.historial());
        }

        boolean sembrado = SemillaDesarrollo.ejecutar();
        System.out.println(sembrado
                ? "Semilla de desarrollo creada (admin/admin)."
                : "Semilla ya presente; nada que hacer.");
        System.out.println("Listo.");
    }
}
