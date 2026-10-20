package ec.sfjfx.core.db;

import ec.sfjfx.core.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fabrica de conexiones JDBC a PostgreSQL.
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
        Connection conexion = DriverManager.getConnection(
                AppConfig.obtener("db.url"),
                AppConfig.obtener("db.usuario"),
                AppConfig.obtener("db.password"));
        aplicarEsquema(conexion, AppConfig.obtener("db.esquema", "public"));
        return conexion;
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

    /**
     * Prueba una configuracion de conexion sin modificar la configuracion global.
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

    private static void aplicarEsquema(Connection conexion, String esquema) throws SQLException {
        String valor = esquema == null || esquema.isBlank() ? "public" : esquema.trim();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT set_config('search_path', ?, false)")) {
            ps.setString(1, valor);
            ps.execute();
        }
    }
}
