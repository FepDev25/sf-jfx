package ec.sfjfx.core.reporte;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.db.Bd;

/** Persistencia de configuraciones de filtros guardadas. */
public final class ReporteGuardadoRepositorio {

    public List<ReporteGuardado> listar(long empresaId, long usuarioId) {
        return Bd.consulta(c -> {
            List<ReporteGuardado> lista = new ArrayList<>();
            String sql = "SELECT id, empresa_id, usuario_id, codigo, nombre, desde, hasta, sucursal_id, "
                    + "categoria_id, filtro_usuario_id FROM reporte_guardado "
                    + "WHERE empresa_id = ? AND (usuario_id = ? OR usuario_id IS NULL) "
                    + "ORDER BY nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setLong(2, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(leer(rs));
                    }
                }
            }
            return lista;
        });
    }

    public long insertar(ReporteGuardado r) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO reporte_guardado (empresa_id, usuario_id, codigo, nombre, desde, hasta, "
                    + "sucursal_id, categoria_id, filtro_usuario_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, r.empresaId());
                ps.setObject(2, r.usuarioId(), Types.BIGINT);
                ps.setString(3, r.codigo());
                ps.setString(4, r.nombre());
                ps.setObject(5, r.desde(), Types.DATE);
                ps.setObject(6, r.hasta(), Types.DATE);
                ps.setObject(7, r.sucursalId(), Types.BIGINT);
                ps.setObject(8, r.categoriaId(), Types.BIGINT);
                ps.setObject(9, r.filtroUsuarioId(), Types.BIGINT);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void eliminar(long empresaId, long id) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM reporte_guardado WHERE id = ? AND empresa_id = ?")) {
                ps.setLong(1, id);
                ps.setLong(2, empresaId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static ReporteGuardado leer(ResultSet rs) throws SQLException {
        return new ReporteGuardado(rs.getLong("id"), rs.getLong("empresa_id"),
                rs.getObject("usuario_id", Long.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getObject("desde", java.time.LocalDate.class),
                rs.getObject("hasta", java.time.LocalDate.class),
                rs.getObject("sucursal_id", Long.class), rs.getObject("categoria_id", Long.class),
                rs.getObject("filtro_usuario_id", Long.class));
    }
}
