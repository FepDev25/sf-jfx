package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Respaldo;

public final class RespaldoRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, ruta_archivo, tamano_bytes, tipo, estado, usuario_id, creado_en, detalle";

    public List<Respaldo> listar(long empresaId, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM respaldo WHERE empresa_id = ? "
                    + "ORDER BY creado_en DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setInt(2, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Respaldo> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public long insertar(Respaldo r) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO respaldo (empresa_id, ruta_archivo, tamano_bytes, tipo, estado, "
                    + "usuario_id, detalle) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, r.empresaId(), Types.BIGINT);
                ps.setString(2, r.rutaArchivo());
                ps.setLong(3, r.tamanoBytes());
                ps.setString(4, r.tipo());
                ps.setString(5, r.estado());
                ps.setObject(6, r.usuarioId(), Types.BIGINT);
                ps.setString(7, r.detalle());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public Optional<Respaldo> ultimoProgramado(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM respaldo WHERE empresa_id = ? "
                    + "AND tipo = 'PROGRAMADO' ORDER BY creado_en DESC LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public void eliminar(long id, long empresaId) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM respaldo WHERE id = ? AND empresa_id = ?")) {
                ps.setLong(1, id);
                ps.setLong(2, empresaId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static Respaldo mapear(ResultSet rs) throws SQLException {
        return new Respaldo(rs.getLong("id"), rs.getObject("empresa_id", Long.class),
                rs.getString("ruta_archivo"), rs.getLong("tamano_bytes"), rs.getString("tipo"),
                rs.getString("estado"), rs.getObject("usuario_id", Long.class),
                rs.getObject("creado_en", LocalDateTime.class), rs.getString("detalle"));
    }
}
