package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.AjusteDetalle;
import ec.sfjfx.core.modelo.AjusteInventario;

public final class AjusteInventarioRepositorio {

    public long insertar(Connection c, AjusteInventario a) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ajuste_inventario (empresa_id, bodega_id, numero, tipo, motivo, estado, usuario_id, nota) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, a.empresaId());
            ps.setLong(2, a.bodegaId());
            ps.setString(3, a.numero());
            ps.setString(4, a.tipo());
            ps.setString(5, a.motivo());
            ps.setString(6, a.estado());
            ps.setObject(7, usuarioActual(), Types.BIGINT);
            ps.setString(8, a.nota());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ajuste_inventario_detalle (ajuste_id, producto_id, lote_id, cantidad, costo_unitario) "
                + "VALUES (?, ?, ?, ?, ?)")) {
            for (AjusteDetalle d : a.detalles()) {
                ps.setLong(1, id);
                ps.setLong(2, d.productoId());
                ps.setObject(3, d.loteId(), Types.BIGINT);
                ps.setBigDecimal(4, d.cantidad());
                ps.setBigDecimal(5, d.costoUnitario());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    public void cambiarEstado(Connection c, long id, String estado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE ajuste_inventario SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public Optional<AjusteInventario> porId(long id) {
        return Bd.consulta(c -> {
            AjusteInventario cabecera = null;
            String sql = "SELECT a.id, a.empresa_id, a.bodega_id, b.nombre AS bodega, a.numero, a.tipo, a.motivo, "
                    + "a.estado, a.fecha, a.nota FROM ajuste_inventario a JOIN bodega b ON b.id = a.bodega_id "
                    + "WHERE a.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cabecera = new AjusteInventario(rs.getLong("id"), rs.getLong("empresa_id"),
                                rs.getLong("bodega_id"), rs.getString("bodega"), rs.getString("numero"),
                                rs.getString("tipo"), rs.getString("motivo"), rs.getString("estado"),
                                rs.getObject("fecha", LocalDateTime.class), rs.getString("nota"), new ArrayList<>());
                    }
                }
            }
            if (cabecera == null) {
                return Optional.empty();
            }
            List<AjusteDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT d.id, d.ajuste_id, d.producto_id, p.nombre AS producto, d.lote_id, d.cantidad, "
                    + "d.costo_unitario FROM ajuste_inventario_detalle d JOIN producto p ON p.id = d.producto_id "
                    + "WHERE d.ajuste_id = ? ORDER BY p.nombre")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new AjusteDetalle(rs.getLong("id"), rs.getLong("ajuste_id"),
                                rs.getLong("producto_id"), rs.getString("producto"),
                                (Long) rs.getObject("lote_id"), rs.getBigDecimal("cantidad"),
                                rs.getBigDecimal("costo_unitario")));
                    }
                }
            }
            return Optional.of(new AjusteInventario(cabecera.id(), cabecera.empresaId(), cabecera.bodegaId(),
                    cabecera.bodega(), cabecera.numero(), cabecera.tipo(), cabecera.motivo(), cabecera.estado(),
                    cabecera.fecha(), cabecera.nota(), detalles));
        });
    }

    public List<AjusteInventario> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT a.id, a.empresa_id, a.bodega_id, b.nombre AS bodega, a.numero, a.tipo, a.motivo, "
                    + "a.estado, a.fecha, a.nota FROM ajuste_inventario a JOIN bodega b ON b.id = a.bodega_id "
                    + "WHERE a.empresa_id = ? AND (? IS NULL OR a.numero ILIKE ?) ORDER BY a.fecha DESC LIMIT 200";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<AjusteInventario> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new AjusteInventario(rs.getLong("id"), rs.getLong("empresa_id"),
                                rs.getLong("bodega_id"), rs.getString("bodega"), rs.getString("numero"),
                                rs.getString("tipo"), rs.getString("motivo"), rs.getString("estado"),
                                rs.getObject("fecha", LocalDateTime.class), rs.getString("nota"), new ArrayList<>()));
                    }
                    return lista;
                }
            }
        });
    }

    private Long usuarioActual() {
        try {
            return ec.sfjfx.core.seguridad.SesionActual.requerida().usuario().id();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
