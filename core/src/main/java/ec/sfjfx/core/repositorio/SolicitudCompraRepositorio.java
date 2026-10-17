package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.SolicitudCompra;
import ec.sfjfx.core.modelo.SolicitudDetalle;

public final class SolicitudCompraRepositorio {

    public long insertar(Connection c, SolicitudCompra s, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO solicitud_compra (empresa_id, sucursal_id, numero, estado, usuario_solicita_id, nota) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, s.empresaId());
            ps.setLong(2, s.sucursalId());
            ps.setString(3, s.numero());
            ps.setString(4, s.estado() == null ? "BORRADOR" : s.estado());
            ps.setObject(5, usuarioId);
            ps.setString(6, s.nota());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO solicitud_compra_detalle (solicitud_id, producto_id, cantidad, justificacion) "
                + "VALUES (?, ?, ?, ?)")) {
            for (SolicitudDetalle d : s.detalles()) {
                ps.setLong(1, id);
                ps.setLong(2, d.productoId());
                ps.setBigDecimal(3, d.cantidad());
                ps.setString(4, d.justificacion());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    public void cambiarEstado(Connection c, long id, String estado, Long usuarioApruebaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE solicitud_compra SET estado = ?, usuario_aprueba_id = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setObject(2, usuarioApruebaId);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public Optional<SolicitudCompra> porId(long id) {
        return Bd.consulta(c -> {
            SolicitudCompra cab = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, empresa_id, sucursal_id, numero, estado, fecha, nota FROM solicitud_compra WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cab = new SolicitudCompra(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("sucursal_id"),
                                rs.getString("numero"), rs.getString("estado"),
                                rs.getObject("fecha", LocalDateTime.class), rs.getString("nota"), new ArrayList<>());
                    }
                }
            }
            if (cab == null) {
                return Optional.empty();
            }
            List<SolicitudDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT d.id, d.solicitud_id, d.producto_id, p.nombre AS producto, d.cantidad, d.justificacion "
                    + "FROM solicitud_compra_detalle d JOIN producto p ON p.id = d.producto_id "
                    + "WHERE d.solicitud_id = ? ORDER BY p.nombre")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new SolicitudDetalle(rs.getLong("id"), rs.getLong("solicitud_id"),
                                rs.getLong("producto_id"), rs.getString("producto"), rs.getBigDecimal("cantidad"),
                                rs.getString("justificacion")));
                    }
                }
            }
            return Optional.of(new SolicitudCompra(cab.id(), cab.empresaId(), cab.sucursalId(), cab.numero(),
                    cab.estado(), cab.fecha(), cab.nota(), detalles));
        });
    }

    public List<SolicitudCompra> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT id, empresa_id, sucursal_id, numero, estado, fecha, nota FROM solicitud_compra "
                    + "WHERE empresa_id = ? AND (? IS NULL OR numero ILIKE ?) ORDER BY id DESC LIMIT 200";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<SolicitudCompra> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new SolicitudCompra(rs.getLong("id"), rs.getLong("empresa_id"),
                                rs.getLong("sucursal_id"), rs.getString("numero"), rs.getString("estado"),
                                rs.getObject("fecha", LocalDateTime.class), rs.getString("nota"), new ArrayList<>()));
                    }
                    return lista;
                }
            }
        });
    }
}
