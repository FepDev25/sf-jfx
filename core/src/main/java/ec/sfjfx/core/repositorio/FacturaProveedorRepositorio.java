package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.FacturaProveedor;

public final class FacturaProveedorRepositorio {

    public long insertar(Connection c, FacturaProveedor f, Long usuarioId) throws SQLException {
        String sql = "INSERT INTO factura_proveedor (empresa_id, proveedor_id, orden_compra_id, recepcion_id, "
                + "tipo_documento, numero, fecha_emision, fecha_vencimiento, subtotal, descuento, iva, total, saldo, "
                + "estado_pago, usuario_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, f.empresaId());
            ps.setLong(2, f.proveedorId());
            ps.setObject(3, f.ordenCompraId(), Types.BIGINT);
            ps.setObject(4, f.recepcionId(), Types.BIGINT);
            ps.setString(5, f.tipoDocumento() == null ? "FACTURA" : f.tipoDocumento());
            ps.setString(6, f.numero());
            ps.setObject(7, f.fechaEmision(), Types.DATE);
            ps.setObject(8, f.fechaVencimiento(), Types.DATE);
            ps.setBigDecimal(9, nz(f.subtotal()));
            ps.setBigDecimal(10, nz(f.descuento()));
            ps.setBigDecimal(11, nz(f.iva()));
            ps.setBigDecimal(12, nz(f.total()));
            ps.setBigDecimal(13, nz(f.total()));
            ps.setString(14, "PENDIENTE");
            ps.setObject(15, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void aplicarPago(Connection c, long facturaId, BigDecimal monto) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE factura_proveedor SET saldo = saldo - ?, estado_pago = CASE "
                + "WHEN saldo - ? <= 0 THEN 'PAGADA' WHEN saldo - ? < total THEN 'PARCIAL' ELSE 'PENDIENTE' END "
                + "WHERE id = ?")) {
            ps.setBigDecimal(1, monto);
            ps.setBigDecimal(2, monto);
            ps.setBigDecimal(3, monto);
            ps.setLong(4, facturaId);
            ps.executeUpdate();
        }
    }

    public boolean numeroExiste(long empresaId, long proveedorId, String numero) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM factura_proveedor WHERE empresa_id = ? AND proveedor_id = ? AND numero = ?")) {
                ps.setLong(1, empresaId);
                ps.setLong(2, proveedorId);
                ps.setString(3, numero);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public Optional<FacturaProveedor> porId(long id) {
        return Bd.consulta(c -> porId(c, id));
    }

    public Optional<FacturaProveedor> porId(Connection c, long id) throws SQLException {
        String sql = "SELECT f.id, f.empresa_id, f.proveedor_id, pr.razon_social AS proveedor, f.orden_compra_id, "
                + "f.recepcion_id, f.tipo_documento, f.numero, f.fecha_emision, f.fecha_vencimiento, f.subtotal, "
                + "f.descuento, f.iva, f.total, f.saldo, f.estado_pago "
                + "FROM factura_proveedor f JOIN proveedor pr ON pr.id = f.proveedor_id WHERE f.id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<FacturaProveedor> listar(long empresaId, String filtro) {
        return listar(empresaId, filtro, null);
    }

    public List<FacturaProveedor> conSaldo(long empresaId, Long proveedorId) {
        return Bd.consulta(c -> {
            String sql = "SELECT f.id, f.empresa_id, f.proveedor_id, pr.razon_social AS proveedor, f.orden_compra_id, "
                    + "f.recepcion_id, f.tipo_documento, f.numero, f.fecha_emision, f.fecha_vencimiento, f.subtotal, "
                    + "f.descuento, f.iva, f.total, f.saldo, f.estado_pago "
                    + "FROM factura_proveedor f JOIN proveedor pr ON pr.id = f.proveedor_id "
                    + "WHERE f.empresa_id = ? AND f.saldo > 0 AND (? IS NULL OR f.proveedor_id = ?) "
                    + "ORDER BY f.fecha_vencimiento NULLS LAST, f.id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setObject(2, proveedorId, Types.BIGINT);
                ps.setObject(3, proveedorId, Types.BIGINT);
                try (ResultSet rs = ps.executeQuery()) {
                    List<FacturaProveedor> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    private List<FacturaProveedor> listar(long empresaId, String filtro, Void ignora) {
        return Bd.consulta(c -> {
            String sql = "SELECT f.id, f.empresa_id, f.proveedor_id, pr.razon_social AS proveedor, f.orden_compra_id, "
                    + "f.recepcion_id, f.tipo_documento, f.numero, f.fecha_emision, f.fecha_vencimiento, f.subtotal, "
                    + "f.descuento, f.iva, f.total, f.saldo, f.estado_pago "
                    + "FROM factura_proveedor f JOIN proveedor pr ON pr.id = f.proveedor_id "
                    + "WHERE f.empresa_id = ? AND (? IS NULL OR f.numero ILIKE ? OR pr.razon_social ILIKE ?) "
                    + "ORDER BY f.id DESC LIMIT 300";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<FacturaProveedor> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    private FacturaProveedor mapear(ResultSet rs) throws SQLException {
        return new FacturaProveedor(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("proveedor_id"),
                rs.getString("proveedor"), (Long) rs.getObject("orden_compra_id"), (Long) rs.getObject("recepcion_id"),
                rs.getString("tipo_documento"), rs.getString("numero"),
                rs.getObject("fecha_emision", LocalDate.class), rs.getObject("fecha_vencimiento", LocalDate.class),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento"), rs.getBigDecimal("iva"),
                rs.getBigDecimal("total"), rs.getBigDecimal("saldo"), rs.getString("estado_pago"));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
