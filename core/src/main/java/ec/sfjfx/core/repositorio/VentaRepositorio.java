package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
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
import ec.sfjfx.core.modelo.Venta;
import ec.sfjfx.core.modelo.VentaDetalle;
import ec.sfjfx.core.modelo.VentaPago;

public final class VentaRepositorio {

    private static final String COLUMNAS =
            "v.id, v.empresa_id, v.sucursal_id, v.caja_id, v.sesion_caja_id, v.numero, v.cliente_id, "
            + "cl.nombres AS cliente_nombres, cl.razon_social AS cliente_razon, v.usuario_id, v.tipo, v.estado, "
            + "v.fecha, v.subtotal, v.descuento_total, v.impuesto_total, v.propina, v.total, v.costo_total, "
            + "v.utilidad, v.autorizado_por_id, v.nota";

    public long insertar(Connection c, Venta v, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO venta (empresa_id, sucursal_id, caja_id, sesion_caja_id, numero, cliente_id, usuario_id, "
                + "tipo, estado, subtotal, descuento_total, impuesto_total, propina, total, costo_total, utilidad, "
                + "autorizado_por_id, nota) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, v.empresaId());
            ps.setLong(2, v.sucursalId());
            ps.setLong(3, v.cajaId());
            ps.setObject(4, v.sesionCajaId(), Types.BIGINT);
            ps.setString(5, v.numero());
            ps.setObject(6, v.clienteId(), Types.BIGINT);
            ps.setObject(7, usuarioId);
            ps.setString(8, v.tipo());
            ps.setString(9, v.estado());
            ps.setBigDecimal(10, nz(v.subtotal()));
            ps.setBigDecimal(11, nz(v.descuentoTotal()));
            ps.setBigDecimal(12, nz(v.impuestoTotal()));
            ps.setBigDecimal(13, nz(v.propina()));
            ps.setBigDecimal(14, nz(v.total()));
            ps.setBigDecimal(15, nz(v.costoTotal()));
            ps.setBigDecimal(16, nz(v.utilidad()));
            ps.setObject(17, v.autorizadoPorId(), Types.BIGINT);
            ps.setString(18, v.nota());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        insertarDetalles(c, id, v.detalles());
        insertarPagos(c, id, v.pagos(), usuarioId);
        return id;
    }

    public void reemplazarDetallesYPagos(Connection c, long ventaId, Venta v, Long usuarioId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM venta_detalle WHERE venta_id = ?")) {
            ps.setLong(1, ventaId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM venta_pago WHERE venta_id = ?")) {
            ps.setLong(1, ventaId);
            ps.executeUpdate();
        }
        insertarDetalles(c, ventaId, v.detalles());
        insertarPagos(c, ventaId, v.pagos(), usuarioId);
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE venta SET estado = ?, subtotal = ?, descuento_total = ?, impuesto_total = ?, propina = ?, "
                + "total = ?, costo_total = ?, utilidad = ?, cliente_id = ?, autorizado_por_id = ?, nota = ?, "
                + "sesion_caja_id = ? WHERE id = ?")) {
            ps.setString(1, v.estado());
            ps.setBigDecimal(2, nz(v.subtotal()));
            ps.setBigDecimal(3, nz(v.descuentoTotal()));
            ps.setBigDecimal(4, nz(v.impuestoTotal()));
            ps.setBigDecimal(5, nz(v.propina()));
            ps.setBigDecimal(6, nz(v.total()));
            ps.setBigDecimal(7, nz(v.costoTotal()));
            ps.setBigDecimal(8, nz(v.utilidad()));
            ps.setObject(9, v.clienteId(), Types.BIGINT);
            ps.setObject(10, v.autorizadoPorId(), Types.BIGINT);
            ps.setString(11, v.nota());
            ps.setObject(12, v.sesionCajaId(), Types.BIGINT);
            ps.setLong(13, ventaId);
            ps.executeUpdate();
        }
    }

    public void cambiarEstado(Connection c, long ventaId, String estado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE venta SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setLong(2, ventaId);
            ps.executeUpdate();
        }
    }

    private void insertarDetalles(Connection c, long ventaId, List<VentaDetalle> detalles) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO venta_detalle (venta_id, producto_id, descripcion, cantidad, precio_unitario, descuento, "
                + "impuesto_id, impuesto_valor, subtotal, total, costo_unitario) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (VentaDetalle d : detalles) {
                ps.setLong(1, ventaId);
                ps.setObject(2, d.productoId(), Types.BIGINT);
                ps.setString(3, d.descripcion());
                ps.setBigDecimal(4, nz(d.cantidad()));
                ps.setBigDecimal(5, nz(d.precioUnitario()));
                ps.setBigDecimal(6, nz(d.descuento()));
                ps.setObject(7, d.impuestoId(), Types.BIGINT);
                ps.setBigDecimal(8, nz(d.impuestoValor()));
                ps.setBigDecimal(9, nz(d.subtotal()));
                ps.setBigDecimal(10, nz(d.total()));
                ps.setBigDecimal(11, nz(d.costoUnitario()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertarPagos(Connection c, long ventaId, List<VentaPago> pagos, Long usuarioId) throws SQLException {
        if (pagos == null || pagos.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO venta_pago (venta_id, medio_pago_id, monto, referencia, cambio, usuario_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)")) {
            for (VentaPago p : pagos) {
                ps.setLong(1, ventaId);
                ps.setLong(2, p.medioPagoId());
                ps.setBigDecimal(3, nz(p.monto()));
                ps.setString(4, p.referencia());
                ps.setBigDecimal(5, nz(p.cambio()));
                ps.setObject(6, usuarioId, Types.BIGINT);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Optional<Venta> porId(long id) {
        return Bd.consulta(c -> {
            Venta cab = null;
            String sql = "SELECT " + COLUMNAS + " FROM venta v LEFT JOIN cliente cl ON cl.id = v.cliente_id "
                    + "WHERE v.id = ?";
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
            return Optional.of(new Venta(cab.id(), cab.empresaId(), cab.sucursalId(), cab.cajaId(), cab.sesionCajaId(),
                    cab.numero(), cab.clienteId(), cab.clienteNombre(), cab.usuarioId(), cab.tipo(), cab.estado(),
                    cab.fecha(), cab.subtotal(), cab.descuentoTotal(), cab.impuestoTotal(), cab.propina(), cab.total(),
                    cab.costoTotal(), cab.utilidad(), cab.autorizadoPorId(), cab.nota(),
                    detallesEn(c, id), pagosEn(c, id)));
        });
    }

    public List<VentaDetalle> detallesEn(Connection c, long ventaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, venta_id, producto_id, descripcion, cantidad, precio_unitario, descuento, impuesto_id, "
                + "impuesto_valor, subtotal, total, costo_unitario FROM venta_detalle WHERE venta_id = ? ORDER BY id")) {
            ps.setLong(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<VentaDetalle> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new VentaDetalle(rs.getLong("id"), rs.getLong("venta_id"),
                            (Long) rs.getObject("producto_id"), rs.getString("descripcion"), rs.getBigDecimal("cantidad"),
                            rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("descuento"),
                            (Long) rs.getObject("impuesto_id"), rs.getBigDecimal("impuesto_valor"),
                            rs.getBigDecimal("subtotal"), rs.getBigDecimal("total"), rs.getBigDecimal("costo_unitario")));
                }
                return lista;
            }
        }
    }

    public List<VentaPago> pagosEn(Connection c, long ventaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT vp.id, vp.venta_id, vp.medio_pago_id, mp.nombre AS medio, vp.monto, vp.referencia, vp.cambio "
                + "FROM venta_pago vp JOIN medio_pago mp ON mp.id = vp.medio_pago_id WHERE vp.venta_id = ? ORDER BY vp.id")) {
            ps.setLong(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<VentaPago> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new VentaPago(rs.getLong("id"), rs.getLong("venta_id"), rs.getLong("medio_pago_id"),
                            rs.getString("medio"), rs.getBigDecimal("monto"), rs.getString("referencia"),
                            rs.getBigDecimal("cambio")));
                }
                return lista;
            }
        }
    }

    public List<Venta> listar(long empresaId, String filtro, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM venta v LEFT JOIN cliente cl ON cl.id = v.cliente_id "
                    + "WHERE v.empresa_id = ? AND (? IS NULL OR v.numero ILIKE ?) AND v.tipo = 'VENTA' "
                    + "ORDER BY v.id DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setInt(4, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Venta> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public List<Venta> aparcadas(long cajaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM venta v LEFT JOIN cliente cl ON cl.id = v.cliente_id "
                    + "WHERE v.caja_id = ? AND v.estado = 'APARCADA' ORDER BY v.id DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, cajaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Venta> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public BigDecimal montoEsperadoEfectivo(long sesionId) {
        return Bd.consulta(c -> {
            String sql = "SELECT (SELECT COALESCE(monto_inicial,0) FROM sesion_caja WHERE id = ?) "
                    + "+ COALESCE(SUM(vp.monto - vp.cambio), 0) FROM venta_pago vp "
                    + "JOIN venta v ON v.id = vp.venta_id JOIN medio_pago mp ON mp.id = vp.medio_pago_id "
                    + "WHERE v.sesion_caja_id = ? AND v.estado = 'PAGADA' AND mp.tipo = 'EFECTIVO'";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, sesionId);
                ps.setLong(2, sesionId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getBigDecimal(1);
                }
            }
        });
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        String cliente = rs.getString("cliente_razon");
        if (cliente == null) {
            cliente = rs.getString("cliente_nombres");
        }
        return new Venta(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("sucursal_id"), rs.getLong("caja_id"),
                (Long) rs.getObject("sesion_caja_id"), rs.getString("numero"), (Long) rs.getObject("cliente_id"),
                cliente, (Long) rs.getObject("usuario_id"), rs.getString("tipo"), rs.getString("estado"),
                rs.getObject("fecha", LocalDateTime.class), rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento_total"),
                rs.getBigDecimal("impuesto_total"), rs.getBigDecimal("propina"), rs.getBigDecimal("total"),
                rs.getBigDecimal("costo_total"), rs.getBigDecimal("utilidad"), (Long) rs.getObject("autorizado_por_id"),
                rs.getString("nota"), List.of(), List.of());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
