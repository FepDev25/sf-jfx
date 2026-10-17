package ec.sfjfx.core.repositorio;

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
import ec.sfjfx.core.modelo.Retencion;
import ec.sfjfx.core.modelo.RetencionDetalle;

public final class RetencionRepositorio {

    public long insertar(Connection c, Retencion r, Long usuarioId) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO retencion (empresa_id, proveedor_id, factura_proveedor_id, establecimiento, "
                + "punto_emision, secuencial, fecha_emision, base_iva, valor_iva, valor_retencion_iva, base_renta, "
                + "valor_retencion_renta, total_retenido, estado) "
                + "VALUES (?, ?, ?, '001', '001', ?, ?, ?, ?, ?, ?, ?, ?, 'EMITIDO') RETURNING id")) {
            ps.setLong(1, r.empresaId());
            ps.setLong(2, r.proveedorId());
            ps.setObject(3, r.facturaProveedorId(), Types.BIGINT);
            ps.setString(4, r.numero());
            ps.setObject(5, r.fechaEmision(), Types.DATE);
            ps.setBigDecimal(6, r.baseIva());
            ps.setBigDecimal(7, r.valorIva());
            ps.setBigDecimal(8, r.valorRetencionIva());
            ps.setBigDecimal(9, r.baseRenta());
            ps.setBigDecimal(10, r.valorRetencionRenta());
            ps.setBigDecimal(11, r.totalRetenido());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO retencion_detalle (retencion_id, tipo_impuesto, codigo_retencion, base_imponible, "
                + "porcentaje, valor_retenido) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (RetencionDetalle d : r.detalles()) {
                ps.setLong(1, id);
                ps.setString(2, d.tipoImpuesto());
                ps.setString(3, d.codigoRetencion());
                ps.setBigDecimal(4, d.baseImponible());
                ps.setBigDecimal(5, d.porcentaje());
                ps.setBigDecimal(6, d.valorRetenido());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return id;
    }

    public Optional<Retencion> porId(long id) {
        return Bd.consulta(c -> {
            Retencion cab = null;
            String sql = "SELECT r.id, r.empresa_id, r.proveedor_id, pr.razon_social AS proveedor, "
                    + "r.factura_proveedor_id, r.secuencial, r.fecha_emision, r.base_iva, r.valor_iva, "
                    + "r.valor_retencion_iva, r.base_renta, r.valor_retencion_renta, r.total_retenido, r.estado "
                    + "FROM retencion r JOIN proveedor pr ON pr.id = r.proveedor_id WHERE r.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cab = new Retencion(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("proveedor_id"),
                                rs.getString("proveedor"), (Long) rs.getObject("factura_proveedor_id"),
                                rs.getString("secuencial"), rs.getObject("fecha_emision", LocalDate.class),
                                rs.getBigDecimal("base_iva"), rs.getBigDecimal("valor_iva"),
                                rs.getBigDecimal("valor_retencion_iva"), rs.getBigDecimal("base_renta"),
                                rs.getBigDecimal("valor_retencion_renta"), rs.getBigDecimal("total_retenido"),
                                rs.getString("estado"), new ArrayList<>());
                    }
                }
            }
            if (cab == null) {
                return Optional.empty();
            }
            List<RetencionDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, retencion_id, tipo_impuesto, codigo_retencion, base_imponible, porcentaje, "
                    + "valor_retenido FROM retencion_detalle WHERE retencion_id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new RetencionDetalle(rs.getLong("id"), rs.getLong("retencion_id"),
                                rs.getString("tipo_impuesto"), rs.getString("codigo_retencion"),
                                rs.getBigDecimal("base_imponible"), rs.getBigDecimal("porcentaje"),
                                rs.getBigDecimal("valor_retenido")));
                    }
                }
            }
            return Optional.of(new Retencion(cab.id(), cab.empresaId(), cab.proveedorId(), cab.proveedorNombre(),
                    cab.facturaProveedorId(), cab.numero(), cab.fechaEmision(), cab.baseIva(), cab.valorIva(),
                    cab.valorRetencionIva(), cab.baseRenta(), cab.valorRetencionRenta(), cab.totalRetenido(),
                    cab.estado(), detalles));
        });
    }

    public List<Retencion> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT r.id, r.empresa_id, r.proveedor_id, pr.razon_social AS proveedor, "
                    + "r.factura_proveedor_id, r.secuencial, r.fecha_emision, r.base_iva, r.valor_iva, "
                    + "r.valor_retencion_iva, r.base_renta, r.valor_retencion_renta, r.total_retenido, r.estado "
                    + "FROM retencion r JOIN proveedor pr ON pr.id = r.proveedor_id "
                    + "WHERE r.empresa_id = ? AND (? IS NULL OR r.secuencial ILIKE ? OR pr.razon_social ILIKE ?) "
                    + "ORDER BY r.id DESC LIMIT 300";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Retencion> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new Retencion(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("proveedor_id"),
                                rs.getString("proveedor"), (Long) rs.getObject("factura_proveedor_id"),
                                rs.getString("secuencial"), rs.getObject("fecha_emision", LocalDate.class),
                                rs.getBigDecimal("base_iva"), rs.getBigDecimal("valor_iva"),
                                rs.getBigDecimal("valor_retencion_iva"), rs.getBigDecimal("base_renta"),
                                rs.getBigDecimal("valor_retencion_renta"), rs.getBigDecimal("total_retenido"),
                                rs.getString("estado"), new ArrayList<>()));
                    }
                    return lista;
                }
            }
        });
    }
}
