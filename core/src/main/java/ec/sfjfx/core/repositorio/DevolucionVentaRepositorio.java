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
import ec.sfjfx.core.modelo.DevolucionVenta;
import ec.sfjfx.core.modelo.DevolucionVentaDetalle;

public final class DevolucionVentaRepositorio {

    public long insertar(Connection c, DevolucionVenta d, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO devolucion_venta (empresa_id, venta_id, cliente_id, numero, motivo, total, bodega_id, "
                + "usuario_id, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'APLICADA') RETURNING id")) {
            ps.setLong(1, d.empresaId());
            ps.setLong(2, d.ventaId());
            ps.setObject(3, d.clienteId(), Types.BIGINT);
            ps.setString(4, d.numero());
            ps.setString(5, d.motivo());
            ps.setBigDecimal(6, d.total());
            ps.setObject(7, d.bodegaId(), Types.BIGINT);
            ps.setObject(8, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO devolucion_venta_detalle (devolucion_id, producto_id, cantidad, precio_unitario, total, "
                + "reingresa_stock) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (DevolucionVentaDetalle det : d.detalles()) {
                ps.setLong(1, id);
                ps.setObject(2, det.productoId(), Types.BIGINT);
                ps.setBigDecimal(3, det.cantidad());
                ps.setBigDecimal(4, det.precioUnitario());
                ps.setBigDecimal(5, det.total());
                ps.setBoolean(6, det.reingresaStock());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    public Optional<DevolucionVenta> porId(long id) {
        return Bd.consulta(c -> {
            DevolucionVenta cab = null;
            String sql = "SELECT d.id, d.empresa_id, d.venta_id, v.numero AS venta_numero, d.cliente_id, d.numero, "
                    + "d.fecha, d.motivo, d.total, d.bodega_id, b.nombre AS bodega, d.estado "
                    + "FROM devolucion_venta d JOIN venta v ON v.id = d.venta_id "
                    + "LEFT JOIN bodega b ON b.id = d.bodega_id WHERE d.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cab = new DevolucionVenta(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("venta_id"),
                                rs.getString("venta_numero"), (Long) rs.getObject("cliente_id"), rs.getString("numero"),
                                rs.getObject("fecha", LocalDateTime.class), rs.getString("motivo"), rs.getBigDecimal("total"),
                                (Long) rs.getObject("bodega_id"), rs.getString("bodega"), rs.getString("estado"),
                                new ArrayList<>());
                    }
                }
            }
            if (cab == null) {
                return Optional.empty();
            }
            List<DevolucionVentaDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT d.id, d.devolucion_id, d.producto_id, p.nombre AS producto, d.cantidad, d.precio_unitario, "
                    + "d.total, d.reingresa_stock FROM devolucion_venta_detalle d "
                    + "LEFT JOIN producto p ON p.id = d.producto_id WHERE d.devolucion_id = ? ORDER BY d.id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new DevolucionVentaDetalle(rs.getLong("id"), rs.getLong("devolucion_id"),
                                rs.getLong("producto_id"), rs.getString("producto"), rs.getBigDecimal("cantidad"),
                                rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("total"),
                                rs.getBoolean("reingresa_stock")));
                    }
                }
            }
            return Optional.of(new DevolucionVenta(cab.id(), cab.empresaId(), cab.ventaId(), cab.ventaNumero(),
                    cab.clienteId(), cab.numero(), cab.fecha(), cab.motivo(), cab.total(), cab.bodegaId(), cab.bodega(),
                    cab.estado(), detalles));
        });
    }

    public List<DevolucionVenta> listar(long empresaId, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT d.id, d.empresa_id, d.venta_id, v.numero AS venta_numero, d.cliente_id, d.numero, "
                    + "d.fecha, d.motivo, d.total, d.bodega_id, b.nombre AS bodega, d.estado "
                    + "FROM devolucion_venta d JOIN venta v ON v.id = d.venta_id "
                    + "LEFT JOIN bodega b ON b.id = d.bodega_id WHERE d.empresa_id = ? ORDER BY d.id DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setInt(2, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<DevolucionVenta> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new DevolucionVenta(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("venta_id"),
                                rs.getString("venta_numero"), (Long) rs.getObject("cliente_id"), rs.getString("numero"),
                                rs.getObject("fecha", LocalDateTime.class), rs.getString("motivo"), rs.getBigDecimal("total"),
                                (Long) rs.getObject("bodega_id"), rs.getString("bodega"), rs.getString("estado"), List.of()));
                    }
                    return lista;
                }
            }
        });
    }
}
