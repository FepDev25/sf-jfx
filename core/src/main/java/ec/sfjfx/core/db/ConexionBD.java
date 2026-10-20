package ec.sfjfx.core.db;

import ec.sfjfx.core.config.AppConfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fabrica de conexiones JDBC a PostgreSQL con pool HikariCP.
 *
 * <p>El pool se crea de forma perezosa con la configuracion actual y se reinicia cuando el
 * asistente de instalacion cambia la conexion. El acceso queda centralizado aqui para permitir
 * pruebas, modo offline u otro pool sin tocar el dominio.
 */
public final class ConexionBD {

    private static volatile HikariDataSource pool;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver de PostgreSQL no encontrado en el classpath", e);
        }
    }

    private ConexionBD() {
    }

    /** Abre una conexion nueva (o reutilizada) del pool. */
    public static Connection abrir() throws SQLException {
        Connection conexion = pool().getConnection();
        try {
            aplicarEsquema(conexion, AppConfig.obtener("db.esquema", "public"));
            return conexion;
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
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
        } catch (SQLException | RuntimeException e) {
            return false;
        }
    }

    /** Version de PostgreSQL del servidor conectado, o {@code "desconocida"}. */
    public static String version() {
        try (Connection conexion = abrir()) {
            return conexion.getMetaData().getDatabaseProductVersion();
        } catch (SQLException | RuntimeException e) {
            return "desconocida";
        }
    }

    /**
     * Prueba una configuracion de conexion sin modificar la configuracion global ni el pool.
     *
     * <p>Se usa desde el asistente de instalacion para validar los datos antes de guardarlos.
     */
    public static void probarConfiguracion(String url, String usuario, String password, String esquema)
            throws SQLException {
        try (Connection conexion = DriverManager.getConnection(url, usuario, password)) {
            aplicarEsquema(conexion, esquema);
            try (Statement st = conexion.createStatement()) {
                st.execute("SELECT 1");
            }
        }
    }

    /**
     * Crea el esquema configurado si no es {@code public} y aun no existe.
     *
     * <p>Se usa antes de aplicar migraciones en una instalacion nueva. La validacion del nombre
     * evita inyeccion SQL porque {@code CREATE SCHEMA} no admite parametros.
     */
    public static void asegurarEsquema() throws SQLException {
        String esquema = AppConfig.obtener("db.esquema", "public").trim();
        if (esquema.isBlank() || "public".equalsIgnoreCase(esquema)) {
            return;
        }
        if (!esquema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new SQLException("Nombre de esquema invalido: " + esquema);
        }
        try (Connection conexion = DriverManager.getConnection(
                AppConfig.obtener("db.url"),
                AppConfig.obtener("db.usuario"),
                AppConfig.obtener("db.password"));
             Statement st = conexion.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS " + esquema);
        }
    }

    /** Reinicia el pool; se llama cuando cambia la configuracion de conexion. */
    public static synchronized void reiniciar() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }

    /** Cierra el pool al terminar la aplicacion. */
    public static void cerrar() {
        reiniciar();
    }

    private static HikariDataSource pool() {
        HikariDataSource actual = pool;
        if (actual == null || actual.isClosed()) {
            synchronized (ConexionBD.class) {
                actual = pool;
                if (actual == null || actual.isClosed()) {
                    actual = crearPool();
                    pool = actual;
                }
            }
        }
        return actual;
    }

    private static HikariDataSource crearPool() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("sfjfx-pool");
        config.setJdbcUrl(AppConfig.obtener("db.url"));
        config.setUsername(AppConfig.obtener("db.usuario"));
        config.setPassword(AppConfig.obtener("db.password"));
        config.setMaximumPoolSize(Math.max(1, AppConfig.obtenerInt("db.pool.maximo", 10)));
        config.setMinimumIdle(Math.max(0, AppConfig.obtenerInt("db.pool.minimo", 1)));
        config.setConnectionTimeout(Math.max(1000, AppConfig.obtenerInt("db.pool.timeout", 30000)));
        config.setValidationTimeout(5000);
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }

    private static void aplicarEsquema(Connection conexion, String esquema) throws SQLException {
        String valor = esquema == null || esquema.isBlank() ? "public" : esquema.trim();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT set_config('search_path', ?, false)")) {
            ps.setString(1, valor);
            ps.execute();
        }
    }
}
