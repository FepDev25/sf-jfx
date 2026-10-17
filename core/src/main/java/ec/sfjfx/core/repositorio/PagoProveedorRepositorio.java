package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.PagoProveedor;

public final class PagoProveedorRepositorio {

    private final FacturaProveedorRepositorio facturas = new FacturaProveedorRepositorio();

    public long insertar(Connection c, PagoProveedor p, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO pago_proveedor (empresa_id, proveedor_id, numero, total, medio_pago_id, referencia, "
                + "usuario_id, nota) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, p.empresaId());
            ps.setLong(2, p.proveedorId());
            ps.setString(3, p.numero());
            ps.setBigDecimal(4, p.total());
            ps.setObject(5, p.medioPagoId(), Types.BIGINT);
            ps.setString(6, p.referencia());
            ps.setObject(7, usuarioId);
            ps.setString(8, p.nota());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO pago_proveedor_factura (pago_id, factura_proveedor_id, monto) VALUES (?, ?, ?)")) {
            for (PagoProveedor.PagoFactura a : p.aplicaciones()) {
                ps.setLong(1, id);
                ps.setLong(2, a.facturaProveedorId());
                ps.setBigDecimal(3, a.monto());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        for (PagoProveedor.PagoFactura a : p.aplicaciones()) {
            facturas.aplicarPago(c, a.facturaProveedorId(), a.monto());
        }
        return id;
    }

    public List<PagoProveedor> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT p.id, p.empresa_id, p.proveedor_id, pr.razon_social AS proveedor, p.numero, "
                    + "p.fecha, p.total, p.medio_pago_id, p.referencia, p.nota "
                    + "FROM pago_proveedor p JOIN proveedor pr ON pr.id = p.proveedor_id "
                    + "WHERE p.empresa_id = ? AND (? IS NULL OR p.numero ILIKE ?) ORDER BY p.id DESC LIMIT 300";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<PagoProveedor> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new PagoProveedor(rs.getLong("id"), rs.getLong("empresa_id"),
                                rs.getLong("proveedor_id"), rs.getString("proveedor"), rs.getString("numero"),
                                rs.getObject("fecha", LocalDateTime.class), rs.getBigDecimal("total"),
                                (Long) rs.getObject("medio_pago_id"), rs.getString("referencia"), rs.getString("nota"),
                                List.of()));
                    }
                    return lista;
                }
            }
        });
    }
}
