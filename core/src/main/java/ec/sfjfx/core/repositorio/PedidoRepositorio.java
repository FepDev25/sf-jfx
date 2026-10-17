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
import ec.sfjfx.core.modelo.Pedido;
import ec.sfjfx.core.modelo.PedidoDetalle;

public final class PedidoRepositorio {

    public long insertar(Connection c, Pedido p, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO pedido (empresa_id, sucursal_id, cliente_id, numero, tipo_entrega, estado, "
                + "direccion_entrega_id, fecha_entrega_estimada, subtotal, descuento, total, nota) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, p.empresaId());
            ps.setLong(2, p.sucursalId());
            ps.setObject(3, p.clienteId(), Types.BIGINT);
            ps.setString(4, p.numero());
            ps.setString(5, p.tipoEntrega());
            ps.setString(6, p.estado() == null ? "RECIBIDO" : p.estado());
            ps.setObject(7, p.direccionEntregaId(), Types.BIGINT);
            ps.setObject(8, p.fechaEntregaEstimada(), Types.TIMESTAMP);
            ps.setBigDecimal(9, nz(p.subtotal()));
            ps.setBigDecimal(10, nz(p.descuento()));
            ps.setBigDecimal(11, nz(p.total()));
            ps.setString(12, p.nota());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO pedido_detalle (pedido_id, producto_id, cantidad, precio_unitario, total) "
                + "VALUES (?, ?, ?, ?, ?)")) {
            for (PedidoDetalle d : p.detalles()) {
                ps.setLong(1, id);
                ps.setObject(2, d.productoId(), Types.BIGINT);
                ps.setBigDecimal(3, nz(d.cantidad()));
                ps.setBigDecimal(4, nz(d.precioUnitario()));
                ps.setBigDecimal(5, nz(d.total()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    public void cambiarEstado(Connection c, long id, String estado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE pedido SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void accionEstado(long id, String estado) {
        Bd.transaccion(c -> {
            cambiarEstado(c, id, estado);
            return null;
        });
    }

    public void vincularVenta(Connection c, long pedidoId, long ventaId, String estado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE pedido SET venta_id = ?, estado = ? WHERE id = ?")) {
            ps.setLong(1, ventaId);
            ps.setString(2, estado);
            ps.setLong(3, pedidoId);
            ps.executeUpdate();
        }
    }

    public Optional<Pedido> porId(long id) {
        return Bd.consulta(c -> {
            Pedido cab = null;
            String sql = "SELECT p.id, p.empresa_id, p.sucursal_id, p.cliente_id, cl.nombres, cl.razon_social, p.numero, "
                    + "p.tipo_entrega, p.estado, p.direccion_entrega_id, d.direccion AS direccion, p.fecha, "
                    + "p.fecha_entrega_estimada, p.subtotal, p.descuento, p.total, p.venta_id, v.numero AS venta_numero, p.nota "
                    + "FROM pedido p LEFT JOIN cliente cl ON cl.id = p.cliente_id "
                    + "LEFT JOIN cliente_direccion d ON d.id = p.direccion_entrega_id "
                    + "LEFT JOIN venta v ON v.id = p.venta_id WHERE p.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cab = mapear(rs);
                    }
                }
            }
            if (cab == null) {
                return Optional.empty();
            }
            List<PedidoDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT d.id, d.pedido_id, d.producto_id, p.nombre AS producto, d.cantidad, d.precio_unitario, d.total "
                    + "FROM pedido_detalle d LEFT JOIN producto p ON p.id = d.producto_id WHERE d.pedido_id = ? ORDER BY d.id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new PedidoDetalle(rs.getLong("id"), rs.getLong("pedido_id"),
                                (Long) rs.getObject("producto_id"), rs.getString("producto"), rs.getBigDecimal("cantidad"),
                                rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("total")));
                    }
                }
            }
            return Optional.of(new Pedido(cab.id(), cab.empresaId(), cab.sucursalId(), cab.clienteId(),
                    cab.clienteNombre(), cab.numero(), cab.tipoEntrega(), cab.estado(), cab.direccionEntregaId(),
                    cab.direccionEntrega(), cab.fecha(), cab.fechaEntregaEstimada(), cab.subtotal(), cab.descuento(),
                    cab.total(), cab.ventaId(), cab.ventaNumero(), cab.nota(), detalles));
        });
    }

    public List<Pedido> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT p.id, p.empresa_id, p.sucursal_id, p.cliente_id, cl.nombres, cl.razon_social, p.numero, "
                    + "p.tipo_entrega, p.estado, p.direccion_entrega_id, d.direccion AS direccion, p.fecha, "
                    + "p.fecha_entrega_estimada, p.subtotal, p.descuento, p.total, p.venta_id, v.numero AS venta_numero, p.nota "
                    + "FROM pedido p LEFT JOIN cliente cl ON cl.id = p.cliente_id "
                    + "LEFT JOIN cliente_direccion d ON d.id = p.direccion_entrega_id "
                    + "LEFT JOIN venta v ON v.id = p.venta_id "
                    + "WHERE p.empresa_id = ? AND (? IS NULL OR p.numero ILIKE ?) ORDER BY p.id DESC LIMIT 300";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Pedido> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        String cliente = rs.getString("razon_social");
        if (cliente == null) {
            cliente = rs.getString("nombres");
        }
        return new Pedido(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("sucursal_id"),
                (Long) rs.getObject("cliente_id"), cliente, rs.getString("numero"), rs.getString("tipo_entrega"),
                rs.getString("estado"), (Long) rs.getObject("direccion_entrega_id"), rs.getString("direccion"),
                rs.getObject("fecha", LocalDateTime.class), rs.getObject("fecha_entrega_estimada", LocalDateTime.class),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento"), rs.getBigDecimal("total"),
                (Long) rs.getObject("venta_id"), rs.getString("venta_numero"), rs.getString("nota"), List.of());
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}
