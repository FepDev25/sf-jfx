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
import ec.sfjfx.core.modelo.DevolucionCompra;
import ec.sfjfx.core.modelo.DevolucionCompraDetalle;

public final class DevolucionCompraRepositorio {

    public long insertar(Connection c, DevolucionCompra d, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO devolucion_compra (empresa_id, proveedor_id, factura_proveedor_id, recepcion_id, "
                + "bodega_id, numero, motivo, total, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'APLICADA') RETURNING id")) {
            ps.setLong(1, d.empresaId());
            ps.setLong(2, d.proveedorId());
            ps.setObject(3, d.facturaProveedorId(), Types.BIGINT);
            ps.setObject(4, d.recepcionId(), Types.BIGINT);
            ps.setLong(5, d.bodegaId());
            ps.setString(6, d.numero());
            ps.setString(7, d.motivo());
            ps.setBigDecimal(8, d.total());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO devolucion_compra_detalle (devolucion_id, producto_id, lote_id, cantidad, "
                + "costo_unitario, total) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (DevolucionCompraDetalle det : d.detalles()) {
                ps.setLong(1, id);
                ps.setLong(2, det.productoId());
                ps.setObject(3, det.loteId(), Types.BIGINT);
                ps.setBigDecimal(4, det.cantidad());
                ps.setBigDecimal(5, det.costoUnitario());
                ps.setBigDecimal(6, det.total());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    public void cambiarEstado(Connection c, long id, String estado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE devolucion_compra SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public Optional<DevolucionCompra> porId(long id) {
        return Bd.consulta(c -> {
            DevolucionCompra cab = null;
            String sql = "SELECT d.id, d.empresa_id, d.proveedor_id, pr.razon_social AS proveedor, "
                    + "d.factura_proveedor_id, d.recepcion_id, d.bodega_id, b.nombre AS bodega, d.numero, d.fecha, "
                    + "d.motivo, d.total, d.estado FROM devolucion_compra d JOIN proveedor pr ON pr.id = d.proveedor_id "
                    + "JOIN bodega b ON b.id = d.bodega_id WHERE d.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cab = new DevolucionCompra(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("proveedor_id"),
                                rs.getString("proveedor"), (Long) rs.getObject("factura_proveedor_id"),
                                (Long) rs.getObject("recepcion_id"), rs.getLong("bodega_id"), rs.getString("bodega"),
                                rs.getString("numero"), rs.getObject("fecha", LocalDateTime.class), rs.getString("motivo"),
                                rs.getBigDecimal("total"), rs.getString("estado"), null, new ArrayList<>());
                    }
                }
            }
            if (cab == null) {
                return Optional.empty();
            }
            List<DevolucionCompraDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT d.id, d.devolucion_id, d.producto_id, p.nombre AS producto, d.lote_id, d.cantidad, "
                    + "d.costo_unitario, d.total FROM devolucion_compra_detalle d JOIN producto p ON p.id = d.producto_id "
                    + "WHERE d.devolucion_id = ? ORDER BY p.nombre")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new DevolucionCompraDetalle(rs.getLong("id"), rs.getLong("devolucion_id"),
                                rs.getLong("producto_id"), rs.getString("producto"), (Long) rs.getObject("lote_id"),
                                rs.getBigDecimal("cantidad"), rs.getBigDecimal("costo_unitario"), rs.getBigDecimal("total")));
                    }
                }
            }
            return Optional.of(new DevolucionCompra(cab.id(), cab.empresaId(), cab.proveedorId(), cab.proveedorNombre(),
                    cab.facturaProveedorId(), cab.recepcionId(), cab.bodegaId(), cab.bodega(), cab.numero(), cab.fecha(),
                    cab.motivo(), cab.total(), cab.estado(), null, detalles));
        });
    }

    public List<DevolucionCompra> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT d.id, d.empresa_id, d.proveedor_id, pr.razon_social AS proveedor, "
                    + "d.factura_proveedor_id, d.recepcion_id, d.bodega_id, b.nombre AS bodega, d.numero, d.fecha, "
                    + "d.motivo, d.total, d.estado FROM devolucion_compra d JOIN proveedor pr ON pr.id = d.proveedor_id "
                    + "JOIN bodega b ON b.id = d.bodega_id "
                    + "WHERE d.empresa_id = ? AND (? IS NULL OR d.numero ILIKE ?) ORDER BY d.id DESC LIMIT 200";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<DevolucionCompra> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new DevolucionCompra(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("proveedor_id"),
                                rs.getString("proveedor"), (Long) rs.getObject("factura_proveedor_id"),
                                (Long) rs.getObject("recepcion_id"), rs.getLong("bodega_id"), rs.getString("bodega"),
                                rs.getString("numero"), rs.getObject("fecha", LocalDateTime.class), rs.getString("motivo"),
                                rs.getBigDecimal("total"), rs.getString("estado"), null, new ArrayList<>()));
                    }
                    return lista;
                }
            }
        });
    }
}
