package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
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
import ec.sfjfx.core.modelo.OrdenCompra;
import ec.sfjfx.core.modelo.OrdenCompraDetalle;

public final class OrdenCompraRepositorio {

    public long insertar(Connection c, OrdenCompra o, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO orden_compra (empresa_id, sucursal_id, proveedor_id, numero, estado, "
                + "fecha_entrega_esperada, condiciones_pago, subtotal, descuento, impuestos, total, usuario_id, nota) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, o.empresaId());
            ps.setLong(2, o.sucursalId());
            ps.setLong(3, o.proveedorId());
            ps.setString(4, o.numero());
            ps.setString(5, o.estado() == null ? "BORRADOR" : o.estado());
            ps.setObject(6, o.fechaEntregaEsperada(), Types.DATE);
            ps.setString(7, o.condicionesPago());
            ps.setBigDecimal(8, nz(o.subtotal()));
            ps.setBigDecimal(9, nz(o.descuento()));
            ps.setBigDecimal(10, nz(o.impuestos()));
            ps.setBigDecimal(11, nz(o.total()));
            ps.setObject(12, usuarioId);
            ps.setString(13, o.nota());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO orden_compra_detalle (orden_compra_id, producto_id, descripcion, cantidad, "
                + "cantidad_recibida, precio_unitario, impuesto_id, descuento, subtotal, total) "
                + "VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?)")) {
            for (OrdenCompraDetalle d : o.detalles()) {
                ps.setLong(1, id);
                ps.setLong(2, d.productoId());
                ps.setString(3, d.descripcion());
                ps.setBigDecimal(4, d.cantidad());
                ps.setBigDecimal(5, nz(d.precioUnitario()));
                ps.setObject(6, d.impuestoId(), Types.BIGINT);
                ps.setBigDecimal(7, nz(d.descuento()));
                ps.setBigDecimal(8, nz(d.subtotal()));
                ps.setBigDecimal(9, nz(d.total()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    public void cambiarEstado(Connection c, long id, String estado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orden_compra SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void sumarRecibida(Connection c, long detalleId, BigDecimal cantidad) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE orden_compra_detalle SET cantidad_recibida = cantidad_recibida + ? WHERE id = ?")) {
            ps.setBigDecimal(1, cantidad);
            ps.setLong(2, detalleId);
            ps.executeUpdate();
        }
    }

    /** Recalcula el estado segun lo recibido. Devuelve el nuevo estado. */
    public String recalcularEstado(Connection c, long ordenId) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal recibido = BigDecimal.ZERO;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT cantidad, cantidad_recibida FROM orden_compra_detalle WHERE orden_compra_id = ?")) {
            ps.setLong(1, ordenId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    total = total.add(nz(rs.getBigDecimal("cantidad")));
                    recibido = recibido.add(nz(rs.getBigDecimal("cantidad_recibida")));
                }
            }
        }
        String estado = "BORRADOR";
        if (recibido.signum() > 0) {
            estado = recibido.compareTo(total) >= 0 ? "RECIBIDA" : "RECEPCION_PARCIAL";
        }
        if (recibido.signum() > 0) {
            cambiarEstado(c, ordenId, estado);
        }
        return estado;
    }

    public Optional<OrdenCompra> porId(long id) {
        return Bd.consulta(c -> {
            OrdenCompra cab = null;
            String sql = "SELECT o.id, o.empresa_id, o.sucursal_id, o.proveedor_id, pr.razon_social AS proveedor, "
                    + "o.numero, o.estado, o.fecha_emision, o.fecha_entrega_esperada, o.condiciones_pago, "
                    + "o.subtotal, o.descuento, o.impuestos, o.total, o.nota "
                    + "FROM orden_compra o JOIN proveedor pr ON pr.id = o.proveedor_id WHERE o.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cab = new OrdenCompra(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("sucursal_id"),
                                rs.getLong("proveedor_id"), rs.getString("proveedor"), rs.getString("numero"),
                                rs.getString("estado"), rs.getObject("fecha_emision", LocalDateTime.class),
                                rs.getObject("fecha_entrega_esperada", LocalDate.class), rs.getString("condiciones_pago"),
                                rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento"), rs.getBigDecimal("impuestos"),
                                rs.getBigDecimal("total"), rs.getString("nota"), new ArrayList<>());
                    }
                }
            }
            if (cab == null) {
                return Optional.empty();
            }
            List<OrdenCompraDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT d.id, d.orden_compra_id, d.producto_id, p.nombre AS producto, d.descripcion, d.cantidad, "
                    + "d.cantidad_recibida, d.precio_unitario, d.impuesto_id, d.descuento, d.subtotal, d.total "
                    + "FROM orden_compra_detalle d JOIN producto p ON p.id = d.producto_id "
                    + "WHERE d.orden_compra_id = ? ORDER BY d.id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new OrdenCompraDetalle(rs.getLong("id"), rs.getLong("orden_compra_id"),
                                rs.getLong("producto_id"), rs.getString("producto"), rs.getString("descripcion"),
                                rs.getBigDecimal("cantidad"), rs.getBigDecimal("cantidad_recibida"),
                                rs.getBigDecimal("precio_unitario"), (Long) rs.getObject("impuesto_id"),
                                rs.getBigDecimal("descuento"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("total")));
                    }
                }
            }
            return Optional.of(new OrdenCompra(cab.id(), cab.empresaId(), cab.sucursalId(), cab.proveedorId(),
                    cab.proveedorNombre(), cab.numero(), cab.estado(), cab.fechaEmision(), cab.fechaEntregaEsperada(),
                    cab.condicionesPago(), cab.subtotal(), cab.descuento(), cab.impuestos(), cab.total(), cab.nota(),
                    detalles));
        });
    }

    public List<OrdenCompra> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT o.id, o.empresa_id, o.sucursal_id, o.proveedor_id, pr.razon_social AS proveedor, "
                    + "o.numero, o.estado, o.fecha_emision, o.fecha_entrega_esperada, o.condiciones_pago, "
                    + "o.subtotal, o.descuento, o.impuestos, o.total, o.nota "
                    + "FROM orden_compra o JOIN proveedor pr ON pr.id = o.proveedor_id "
                    + "WHERE o.empresa_id = ? AND (? IS NULL OR o.numero ILIKE ?) ORDER BY o.id DESC LIMIT 200";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<OrdenCompra> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new OrdenCompra(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("sucursal_id"),
                                rs.getLong("proveedor_id"), rs.getString("proveedor"), rs.getString("numero"),
                                rs.getString("estado"), rs.getObject("fecha_emision", LocalDateTime.class),
                                rs.getObject("fecha_entrega_esperada", LocalDate.class), rs.getString("condiciones_pago"),
                                rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento"), rs.getBigDecimal("impuestos"),
                                rs.getBigDecimal("total"), rs.getString("nota"), new ArrayList<>()));
                    }
                    return lista;
                }
            }
        });
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
