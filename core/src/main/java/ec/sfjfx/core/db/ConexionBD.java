package ec.sfjfx.core.db;

import ec.sfjfx.core.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fabrica de conexiones JDBC a PostgreSQL. Sin ORM ni pool externo: se usa
 * {@link DriverManager} directamente (filosofia "Java core").
 *
 * <p>El acceso queda centralizado aqui para permitir mas adelante un pool
 * (HikariCP) o modo offline sin tocar el dominio.
 */
public final class ConexionBD {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver de PostgreSQL no encontrado en el classpath", e);
        }
    }

    private ConexionBD() {
    }

    /** Abre una conexion nueva usando la configuracion actual. */
    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.obtener("db.url"),
                AppConfig.obtener("db.usuario"),
                AppConfig.obtener("db.password"));
    }

    /** Abre una conexion en modo transaccional (autocommit desactivado). */
    public static Connection abrirTransaccional() throws SQLException {
        Connection conexion = abrir();
        conexion.setAutoCommit(false);
        return conexion;
    }

    /** Prueba la conexion. Devuelve {@code false} si no es posible conectar. */
    public static boolean probar() {
        try (Connection conexion = abrir()) {
            return conexion.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Version de PostgreSQL del servidor conectado, o {@code "desconocida"}. */
    public static String version() {
        try (Connection conexion = abrir()) {
            return conexion.getMetaData().getDatabaseProductVersion();
        } catch (SQLException e) {
            return "desconocida";
        }
    }
}
