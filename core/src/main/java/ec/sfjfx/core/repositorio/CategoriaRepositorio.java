package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Categoria;

public final class CategoriaRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, padre_id, codigo, nombre, descripcion, orden, activo";

    public List<Categoria> listar(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM categoria WHERE empresa_id = ? ORDER BY orden, nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Categoria> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Categoria> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM categoria WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean codigoExiste(long empresaId, String codigo, Long excluirId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM categoria WHERE empresa_id = ? AND lower(codigo) = lower(?) "
                    + "AND (? IS NULL OR id <> ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, codigo);
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

    public long crear(Categoria cat) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO categoria (empresa_id, padre_id, codigo, nombre, descripcion, orden, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, cat);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Categoria cat) {
        Bd.transaccion(c -> {
            String sql = "UPDATE categoria SET padre_id = ?, codigo = ?, nombre = ?, descripcion = ?, "
                    + "orden = ?, activo = ?, actualizado_en = now() WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, cat.padreId());
                ps.setString(2, cat.codigo());
                ps.setString(3, cat.nombre());
                ps.setString(4, cat.descripcion());
                ps.setInt(5, cat.orden());
                ps.setBoolean(6, cat.activo());
                ps.setLong(7, cat.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void asignar(PreparedStatement ps, Categoria cat) throws SQLException {
        ps.setLong(1, cat.empresaId());
        ps.setObject(2, cat.padreId());
        ps.setString(3, cat.codigo());
        ps.setString(4, cat.nombre());
        ps.setString(5, cat.descripcion());
        ps.setInt(6, cat.orden());
        ps.setBoolean(7, cat.activo());
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        return new Categoria(
                rs.getLong("id"),
                rs.getLong("empresa_id"),
                (Long) rs.getObject("padre_id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getInt("orden"),
                rs.getBoolean("activo"));
    }
}
