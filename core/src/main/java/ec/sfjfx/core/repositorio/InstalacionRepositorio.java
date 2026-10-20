package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Instalacion;

/** Metadatos de la instalacion (una fila por base de datos). */
public final class InstalacionRepositorio {

    private static final String COLUMNAS =
            "id, codigo_unico, nombre, modo, version_esquema, creado_en, actualizado_en";

    public Optional<Instalacion> obtener() {
        return Bd.consulta(this::obtener);
    }

    public Optional<Instalacion> obtener(Connection c) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM instalacion ORDER BY id LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
        }
    }

    public boolean existe() {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM instalacion LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        });
    }

    /** Inserta la fila de instalacion dentro de una transaccion existente. */
    public long insertar(Connection c, String nombre, String modo, String versionEsquema)
            throws SQLException {
        String sql = "INSERT INTO instalacion (nombre, modo, version_esquema) "
                + "VALUES (?, ?, ?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, modo);
            ps.setString(3, versionEsquema);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private Instalacion mapear(ResultSet rs) throws SQLException {
        return new Instalacion(
                rs.getLong("id"),
                rs.getString("codigo_unico"),
                rs.getString("nombre"),
                rs.getString("modo"),
                rs.getString("version_esquema"),
                rs.getObject("creado_en", java.time.LocalDateTime.class),
                rs.getObject("actualizado_en", java.time.LocalDateTime.class));
    }
}
