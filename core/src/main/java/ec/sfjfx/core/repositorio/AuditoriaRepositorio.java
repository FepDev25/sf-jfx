package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.RegistroAuditoria;

public final class AuditoriaRepositorio {

    public void registrar(long empresaId, Long sucursalId, Long usuarioId, String terminal,
                          String modulo, String entidad, Long entidadId, String accion,
                          String valoresAnteriores, String valoresNuevos) {
        Bd.transaccion(c -> {
            String sql = "INSERT INTO auditoria (empresa_id, sucursal_id, usuario_id, terminal, modulo, "
                    + "entidad, entidad_id, accion, valores_anteriores, valores_nuevos) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                if (sucursalId == null) {
                    ps.setNull(2, Types.BIGINT);
                } else {
                    ps.setLong(2, sucursalId);
                }
                if (usuarioId == null) {
                    ps.setNull(3, Types.BIGINT);
                } else {
                    ps.setLong(3, usuarioId);
                }
                ps.setString(4, terminal);
                ps.setString(5, modulo);
                ps.setString(6, entidad);
                if (entidadId == null) {
                    ps.setNull(7, Types.BIGINT);
                } else {
                    ps.setLong(7, entidadId);
                }
                ps.setString(8, accion);
                ps.setString(9, valoresAnteriores);
                ps.setString(10, valoresNuevos);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public List<RegistroAuditoria> listar(long empresaId, String modulo, String entidad, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT a.id, a.usuario_id, u.nombre_usuario, a.terminal, a.modulo, a.entidad, "
                    + "a.entidad_id, a.accion, a.valores_anteriores::text, a.valores_nuevos::text, a.creado_en "
                    + "FROM auditoria a LEFT JOIN usuario u ON u.id = a.usuario_id "
                    + "WHERE a.empresa_id = ? "
                    + "AND (? IS NULL OR a.modulo = ?) "
                    + "AND (? IS NULL OR a.entidad = ?) "
                    + "ORDER BY a.creado_en DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setString(2, modulo);
                ps.setString(3, modulo);
                ps.setString(4, entidad);
                ps.setString(5, entidad);
                ps.setInt(6, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<RegistroAuditoria> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new RegistroAuditoria(
                                rs.getLong("id"),
                                (Long) rs.getObject("usuario_id"),
                                rs.getString("nombre_usuario"),
                                rs.getString("terminal"),
                                rs.getString("modulo"),
                                rs.getString("entidad"),
                                (Long) rs.getObject("entidad_id"),
                                rs.getString("accion"),
                                rs.getString("valores_anteriores"),
                                rs.getString("valores_nuevos"),
                                rs.getObject("creado_en", LocalDateTime.class)));
                    }
                    return lista;
                }
            } catch (SQLException e) {
                throw new RepositorioExcepcion("Error al listar auditoria", e);
            }
        });
    }

    public List<String> modulosDistintos(long empresaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT DISTINCT modulo FROM auditoria WHERE empresa_id = ? ORDER BY modulo")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(rs.getString(1));
                    }
                    return lista;
                }
            }
        });
    }
}
