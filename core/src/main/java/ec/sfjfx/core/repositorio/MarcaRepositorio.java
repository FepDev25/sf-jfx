package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Marca;

public final class MarcaRepositorio {

    private static final String COLUMNAS = "id, empresa_id, nombre, descripcion, logo_path, activo";

    public List<Marca> listar(long empresaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM marca WHERE empresa_id = ? ORDER BY nombre")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Marca> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Marca> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM marca WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean nombreExiste(long empresaId, String nombre, Long excluirId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM marca WHERE empresa_id = ? AND lower(nombre) = lower(?) "
                    + "AND (? IS NULL OR id <> ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, nombre);
                if (excluirId == null) {
                    ps.setNull(3, java.sql.Types.BIGINT);
                    ps.setNull(4, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(3, excluirId);
                    ps.setLong(4, excluirId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public long crear(Marca m) {
        return Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO marca (empresa_id, nombre, descripcion, logo_path, activo) "
                    + "VALUES (?, ?, ?, ?, ?) RETURNING id")) {
                ps.setLong(1, m.empresaId());
                ps.setString(2, m.nombre());
                ps.setString(3, m.descripcion());
                ps.setString(4, m.logoPath());
                ps.setBoolean(5, m.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Marca m) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE marca SET nombre = ?, descripcion = ?, logo_path = ?, activo = ? WHERE id = ?")) {
                ps.setString(1, m.nombre());
                ps.setString(2, m.descripcion());
                ps.setString(3, m.logoPath());
                ps.setBoolean(4, m.activo());
                ps.setLong(5, m.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Marca mapear(ResultSet rs) throws SQLException {
        return new Marca(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("nombre"),
                rs.getString("descripcion"), rs.getString("logo_path"), rs.getBoolean("activo"));
    }
}
