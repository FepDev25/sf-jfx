package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.RecepcionCompra;
import ec.sfjfx.core.modelo.RecepcionDetalle;

public final class RecepcionCompraRepositorio {

    public long insertar(Connection c, RecepcionCompra r, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO recepcion_compra (empresa_id, orden_compra_id, numero, bodega_id, estado, usuario_id, nota) "
                + "VALUES (?, ?, ?, ?, 'CONFIRMADA', ?, ?) RETURNING id")) {
            ps.setLong(1, r.empresaId());
            ps.setObject(2, r.ordenCompraId(), Types.BIGINT);
            ps.setString(3, r.numero());
            ps.setLong(4, r.bodegaId());
            ps.setObject(5, usuarioId);
            ps.setString(6, r.nota());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO recepcion_compra_detalle (recepcion_id, orden_compra_detalle_id, producto_id, lote_id, "
                + "lote_codigo, fecha_vencimiento, cantidad_recibida, costo_unitario) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (RecepcionDetalle d : r.detalles()) {
                ps.setLong(1, id);
                ps.setObject(2, d.ordenCompraDetalleId(), Types.BIGINT);
                ps.setLong(3, d.productoId());
                ps.setObject(4, d.loteId(), Types.BIGINT);
                ps.setString(5, d.loteCodigo());
                ps.setObject(6, d.fechaVencimiento(), Types.DATE);
                ps.setBigDecimal(7, d.cantidadRecibida());
                ps.setBigDecimal(8, d.costoUnitario());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    public Optional<RecepcionCompra> porId(long id) {
        return Bd.consulta(c -> {
            RecepcionCompra cab = null;
            String sql = "SELECT r.id, r.empresa_id, r.orden_compra_id, r.numero, r.bodega_id, b.nombre AS bodega, "
                    + "r.fecha, r.estado, r.nota FROM recepcion_compra r JOIN bodega b ON b.id = r.bodega_id "
                    + "WHERE r.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cab = new RecepcionCompra(rs.getLong("id"), rs.getLong("empresa_id"),
                                (Long) rs.getObject("orden_compra_id"), rs.getString("numero"), rs.getLong("bodega_id"),
                                rs.getString("bodega"), rs.getObject("fecha", LocalDateTime.class),
                                rs.getString("estado"), rs.getString("nota"), new ArrayList<>());
                    }
                }
            }
            if (cab == null) {
                return Optional.empty();
            }
            List<RecepcionDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT d.id, d.recepcion_id, d.orden_compra_detalle_id, d.producto_id, p.nombre AS producto, "
                    + "d.lote_id, d.lote_codigo, d.fecha_vencimiento, d.cantidad_recibida, d.costo_unitario "
                    + "FROM recepcion_compra_detalle d JOIN producto p ON p.id = d.producto_id "
                    + "WHERE d.recepcion_id = ? ORDER BY d.id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new RecepcionDetalle(rs.getLong("id"), rs.getLong("recepcion_id"),
                                (Long) rs.getObject("orden_compra_detalle_id"), rs.getLong("producto_id"),
                                rs.getString("producto"), (Long) rs.getObject("lote_id"), rs.getString("lote_codigo"),
                                rs.getObject("fecha_vencimiento", LocalDate.class),
                                rs.getBigDecimal("cantidad_recibida"), rs.getBigDecimal("costo_unitario")));
                    }
                }
            }
            return Optional.of(new RecepcionCompra(cab.id(), cab.empresaId(), cab.ordenCompraId(), cab.numero(),
                    cab.bodegaId(), cab.bodega(), cab.fecha(), cab.estado(), cab.nota(), detalles));
        });
    }

    public List<RecepcionCompra> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT r.id, r.empresa_id, r.orden_compra_id, r.numero, r.bodega_id, b.nombre AS bodega, "
                    + "r.fecha, r.estado, r.nota FROM recepcion_compra r JOIN bodega b ON b.id = r.bodega_id "
                    + "WHERE r.empresa_id = ? AND (? IS NULL OR r.numero ILIKE ?) ORDER BY r.id DESC LIMIT 200";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<RecepcionCompra> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new RecepcionCompra(rs.getLong("id"), rs.getLong("empresa_id"),
                                (Long) rs.getObject("orden_compra_id"), rs.getString("numero"), rs.getLong("bodega_id"),
                                rs.getString("bodega"), rs.getObject("fecha", LocalDateTime.class),
                                rs.getString("estado"), rs.getString("nota"), new ArrayList<>()));
                    }
                    return lista;
                }
            }
        });
    }
}
