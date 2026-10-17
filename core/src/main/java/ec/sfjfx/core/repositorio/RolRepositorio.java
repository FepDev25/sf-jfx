package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Rol;

public final class RolRepositorio {

    private static final String COLUMNAS = "id, empresa_id, nombre, descripcion, es_sistema, activo";

    public List<Rol> listar(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM rol WHERE empresa_id = ? OR empresa_id IS NULL "
                    + "ORDER BY es_sistema DESC, nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Rol> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Rol> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM rol WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean nombreExiste(String nombre, long empresaId, Long excluirId) {
        return Bd.consulta(c -> {
            String sql = "SELECT 1 FROM rol WHERE lower(nombre) = lower(?) AND empresa_id = ? "
                    + "AND (? IS NULL OR id <> ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setLong(2, empresaId);
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

    public long crear(Rol r) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO rol (empresa_id, nombre, descripcion, es_sistema, activo) "
                    + "VALUES (?, ?, ?, FALSE, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, r.empresaId());
                ps.setString(2, r.nombre());
                ps.setString(3, r.descripcion());
                ps.setBoolean(4, r.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Rol r) {
        Bd.transaccion(c -> {
            String sql = "UPDATE rol SET nombre = ?, descripcion = ?, activo = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, r.nombre());
                ps.setString(2, r.descripcion());
                ps.setBoolean(3, r.activo());
                ps.setLong(4, r.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void eliminar(long id) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM rol WHERE id = ? AND es_sistema = FALSE")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Set<String> permisosDe(long rolId) {
        return Bd.consulta(c -> {
            String sql = "SELECT p.codigo FROM permiso p JOIN rol_permiso rp ON rp.permiso_id = p.id "
                    + "WHERE rp.rol_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, rolId);
                try (ResultSet rs = ps.executeQuery()) {
                    Set<String> codigos = new HashSet<>();
                    while (rs.next()) {
                        codigos.add(rs.getString(1));
                    }
                    return codigos;
                }
            }
        });
    }

    public void asignarPermisos(long rolId, Set<String> codigos) {
        Bd.transaccion(c -> {
            try (PreparedStatement del = c.prepareStatement("DELETE FROM rol_permiso WHERE rol_id = ?")) {
                del.setLong(1, rolId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO rol_permiso (rol_id, permiso_id) "
                    + "SELECT ?, id FROM permiso WHERE codigo = ?")) {
                for (String codigo : codigos) {
                    ins.setLong(1, rolId);
                    ins.setString(2, codigo);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            return null;
        });
    }

    public int contarUsuariosConRol(long rolId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT count(*) FROM usuario_rol WHERE rol_id = ?")) {
                ps.setLong(1, rolId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    private Rol mapear(ResultSet rs) throws SQLException {
        return new Rol(
                rs.getLong("id"),
                (Long) rs.getObject("empresa_id"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getBoolean("es_sistema"),
                rs.getBoolean("activo"));
    }
}
