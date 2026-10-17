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
import ec.sfjfx.core.modelo.Comprobante;
import ec.sfjfx.core.modelo.ComprobanteDetalle;
import ec.sfjfx.core.modelo.ComprobantePago;

public final class ComprobanteRepositorio {

    public long insertar(Connection c, Comprobante cb, Long usuarioId) throws SQLException {
        long id;
        String sql = "INSERT INTO comprobante (empresa_id, sucursal_id, tipo, establecimiento, punto_emision, "
                + "secuencial, numero_completo, clave_acceso, fecha_emision, ambiente, tipo_emision, emisor_ruc, "
                + "emisor_razon_social, emisor_direccion, receptor_tipo_id, receptor_numero_id, receptor_razon_social, "
                + "receptor_email, subtotal, descuento, iva, ice, irbpnr, propina, total, estado, venta_id, "
                + "factura_proveedor_id, motivo, xml_path, xml_firmado_path, pdf_path) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, cb.empresaId());
            ps.setLong(i++, cb.sucursalId());
            ps.setString(i++, cb.tipo());
            ps.setString(i++, cb.establecimiento());
            ps.setString(i++, cb.puntoEmision());
            ps.setString(i++, cb.secuencial());
            ps.setString(i++, cb.numeroCompletoCalculado());
            ps.setString(i++, cb.claveAcceso());
            ps.setObject(i++, cb.fechaEmision(), Types.DATE);
            ps.setString(i++, cb.ambiente());
            ps.setString(i++, cb.tipoEmision());
            ps.setString(i++, cb.emisorRuc());
            ps.setString(i++, cb.emisorRazonSocial());
            ps.setString(i++, cb.emisorDireccion());
            ps.setString(i++, cb.receptorTipoId());
            ps.setString(i++, cb.receptorNumeroId());
            ps.setString(i++, cb.receptorRazonSocial());
            ps.setString(i++, cb.receptorEmail());
            ps.setBigDecimal(i++, nz(cb.subtotal()));
            ps.setBigDecimal(i++, nz(cb.descuento()));
            ps.setBigDecimal(i++, nz(cb.iva()));
            ps.setBigDecimal(i++, nz(cb.ice()));
            ps.setBigDecimal(i++, nz(cb.irbpnr()));
            ps.setBigDecimal(i++, nz(cb.propina()));
            ps.setBigDecimal(i++, nz(cb.total()));
            ps.setString(i++, cb.estado());
            ps.setObject(i++, cb.ventaId(), Types.BIGINT);
            ps.setObject(i++, cb.facturaProveedorId(), Types.BIGINT);
            ps.setString(i++, cb.motivo());
            ps.setString(i++, cb.xmlPath());
            ps.setString(i++, cb.xmlFirmadoPath());
            ps.setString(i, cb.pdfPath());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO comprobante_detalle (comprobante_id, codigo_principal, codigo_auxiliar, descripcion, "
                + "cantidad, precio_unitario, descuento, subtotal, impuesto_total, total) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (ComprobanteDetalle d : cb.detalles()) {
                ps.setLong(1, id);
                ps.setString(2, d.codigoPrincipal());
                ps.setString(3, d.codigoAuxiliar());
                ps.setString(4, d.descripcion());
                ps.setBigDecimal(5, nz(d.cantidad()));
                ps.setBigDecimal(6, nz(d.precioUnitario()));
                ps.setBigDecimal(7, nz(d.descuento()));
                ps.setBigDecimal(8, nz(d.subtotal()));
                ps.setBigDecimal(9, nz(d.impuestoValor()));
                ps.setBigDecimal(10, nz(d.total()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        if (cb.pagos() != null && !cb.pagos().isEmpty()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO comprobante_pago (comprobante_id, forma_pago_codigo, total) VALUES (?, ?, ?)")) {
                for (ComprobantePago p : cb.pagos()) {
                    ps.setLong(1, id);
                    ps.setString(2, p.formaPagoCodigo());
                    ps.setBigDecimal(3, nz(p.total()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        return id;
    }

    public void actualizarArchivos(Connection c, long id, String xmlPath, String xmlFirmadoPath, String pdfPath)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE comprobante SET xml_path = ?, xml_firmado_path = ?, pdf_path = ?, actualizado_en = now() "
                + "WHERE id = ?")) {
            ps.setString(1, xmlPath);
            ps.setString(2, xmlFirmadoPath);
            ps.setString(3, pdfPath);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    public void cambiarEstado(Connection c, long id, String estado, String motivo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE comprobante SET estado = ?, motivo = ?, actualizado_en = now() WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setString(2, motivo);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public Optional<Comprobante> porId(long id) {
        return Bd.consulta(c -> {
            Comprobante cab = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM comprobante WHERE id = ?")) {
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
            List<ComprobanteDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, comprobante_id, codigo_principal, codigo_auxiliar, descripcion, cantidad, "
                    + "precio_unitario, descuento, subtotal, impuesto_total, total FROM comprobante_detalle "
                    + "WHERE comprobante_id = ? ORDER BY id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new ComprobanteDetalle(rs.getLong("id"), rs.getLong("comprobante_id"),
                                rs.getString("codigo_principal"), rs.getString("codigo_auxiliar"),
                                rs.getString("descripcion"), rs.getBigDecimal("cantidad"),
                                rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("descuento"),
                                rs.getBigDecimal("subtotal"), null, rs.getBigDecimal("impuesto_total"),
                                rs.getBigDecimal("total")));
                    }
                }
            }
            List<ComprobantePago> pagos = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT forma_pago_codigo, total FROM comprobante_pago WHERE comprobante_id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        pagos.add(new ComprobantePago(rs.getString("forma_pago_codigo"), rs.getBigDecimal("total")));
                    }
                }
            }
            return Optional.of(new Comprobante(cab.id(), cab.empresaId(), cab.sucursalId(), cab.tipo(),
                    cab.establecimiento(), cab.puntoEmision(), cab.secuencial(), cab.numeroCompleto(), cab.claveAcceso(),
                    cab.fechaEmision(), cab.ambiente(), cab.tipoEmision(), cab.emisorRuc(), cab.emisorRazonSocial(),
                    null, cab.emisorDireccion(), cab.obligadoContabilidad(), null, cab.receptorTipoId(),
                    cab.receptorNumeroId(), cab.receptorRazonSocial(), cab.receptorEmail(), cab.subtotal(),
                    cab.descuento(), cab.iva(), cab.ice(), cab.irbpnr(), cab.propina(), cab.total(), cab.estado(),
                    cab.ventaId(), cab.ventaNumero(), cab.facturaProveedorId(), cab.motivo(), cab.xmlPath(),
                    cab.xmlFirmadoPath(), cab.pdfPath(), null, detalles, pagos));
        });
    }

    public Optional<Comprobante> facturaDeVenta(long ventaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM comprobante WHERE venta_id = ? AND tipo = 'FACTURA' ORDER BY id DESC LIMIT 1")) {
                ps.setLong(1, ventaId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public List<Comprobante> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT * FROM comprobante WHERE empresa_id = ? "
                    + "AND (? IS NULL OR numero_completo ILIKE ? OR clave_acceso ILIKE ? OR receptor_razon_social ILIKE ?) "
                    + "ORDER BY id DESC LIMIT 300";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                ps.setString(5, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Comprobante> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    private Comprobante mapear(ResultSet rs) throws SQLException {
        return new Comprobante(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("sucursal_id"),
                rs.getString("tipo"), rs.getString("establecimiento"), rs.getString("punto_emision"),
                rs.getString("secuencial"), rs.getString("numero_completo"), rs.getString("clave_acceso"),
                rs.getObject("fecha_emision", LocalDate.class), rs.getString("ambiente"), rs.getString("tipo_emision"),
                rs.getString("emisor_ruc"), rs.getString("emisor_razon_social"), null, rs.getString("emisor_direccion"),
                null, null, rs.getString("receptor_tipo_id"), rs.getString("receptor_numero_id"),
                rs.getString("receptor_razon_social"), rs.getString("receptor_email"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("descuento"), rs.getBigDecimal("iva"), rs.getBigDecimal("ice"),
                rs.getBigDecimal("irbpnr"), rs.getBigDecimal("propina"), rs.getBigDecimal("total"),
                rs.getString("estado"), (Long) rs.getObject("venta_id"), null, (Long) rs.getObject("factura_proveedor_id"),
                rs.getString("motivo"), rs.getString("xml_path"), rs.getString("xml_firmado_path"),
                rs.getString("pdf_path"), null, List.of(), List.of());
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}
