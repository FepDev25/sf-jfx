package ec.sfjfx.core.db;

import java.sql.Connection;
import java.sql.SQLException;

import ec.sfjfx.core.repositorio.RepositorioExcepcion;

/** Helper funcional para ejecutar trabajo JDBC con manejo de conexion y transaccion. */
public final class Bd {

    @FunctionalInterface
    public interface Trabajo<T> {
        T aplicar(Connection conexion) throws SQLException;
    }

    private Bd() {
    }

    /** Ejecuta en una transaccion (commit/rollback automatico). */
    public static <T> T transaccion(Trabajo<T> trabajo) {
        try (Connection c = ConexionBD.abrirTransaccional()) {
            try {
                T resultado = trabajo.aplicar(c);
                c.commit();
                return resultado;
            } catch (SQLException e) {
                c.rollback();
                throw new RepositorioExcepcion("Error de base de datos", e);
            }
        } catch (SQLException e) {
            throw new RepositorioExcepcion("Error de conexion", e);
        }
    }

    /** Ejecuta una consulta de solo lectura. */
    public static <T> T consulta(Trabajo<T> trabajo) {
        try (Connection c = ConexionBD.abrir()) {
            return trabajo.aplicar(c);
        } catch (SQLException e) {
            throw new RepositorioExcepcion("Error de conexion", e);
        }
    }
}
