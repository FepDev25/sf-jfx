package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.ProductoProveedor;
import ec.sfjfx.core.modelo.Proveedor;

public final class ProveedorRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, tipo_identificacion, numero_identificacion, razon_social, nombre_comercial, "
            + "direccion, telefono, email, tipo_contribuyente, dias_credito, aplica_retencion_iva, "
            + "aplica_retencion_renta, activo, nota";

    public List<Proveedor> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM proveedor WHERE empresa_id = ? "
                    + "AND (? IS NULL OR razon_social ILIKE ? OR numero_identificacion ILIKE ?) ORDER BY razon_social";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Proveedor> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Pagina<Proveedor> paginar(long empresaId, String filtro, int pagina, int tamano) {
        int p = Math.max(1, pagina);
        int t = Math.min(200, Math.max(1, tamano));
        return Bd.consulta(c -> {
            String patron = (filtro == null || filtro.isBlank()) ? null : "%" + filtro.trim() + "%";
            long total;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM proveedor WHERE empresa_id = ? "
                    + "AND (? IS NULL OR razon_social ILIKE ? OR numero_identificacion ILIKE ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            List<Proveedor> lista = new ArrayList<>();
            String sql = "SELECT " + COLUMNAS + " FROM proveedor WHERE empresa_id = ? "
                    + "AND (? IS NULL OR razon_social ILIKE ? OR numero_identificacion ILIKE ?) "
                    + "ORDER BY razon_social OFFSET ? LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                ps.setInt(5, (p - 1) * t);
                ps.setInt(6, t);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                }
            }
            return new Pagina<>(lista, total, p, t);
        });
    }

    public Optional<Proveedor> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNAS + " FROM proveedor WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean identificacionExiste(long empresaId, String numero, Long excluirId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM proveedor WHERE empresa_id = ? AND numero_identificacion = ? "
                    + "AND (? IS NULL OR id <> ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, numero);
                if (excluirId == null) {
                    ps.setNull(3, Types.BIGINT);
                    ps.setNull(4, Types.BIGINT);
                } else {
                    ps.setLong(3, excluirId);
                    ps.setLong(4, excluirId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public long crear(Proveedor p) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO proveedor (empresa_id, tipo_identificacion, numero_identificacion, "
                    + "razon_social, nombre_comercial, direccion, telefono, email, tipo_contribuyente, dias_credito, "
                    + "aplica_retencion_iva, aplica_retencion_renta, activo, nota) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, p);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Proveedor p) {
        Bd.transaccion(c -> {
            String sql = "UPDATE proveedor SET tipo_identificacion = ?, numero_identificacion = ?, razon_social = ?, "
                    + "nombre_comercial = ?, direccion = ?, telefono = ?, email = ?, tipo_contribuyente = ?, "
                    + "dias_credito = ?, aplica_retencion_iva = ?, aplica_retencion_renta = ?, activo = ?, nota = ? "
                    + "WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, p.tipoIdentificacion());
                ps.setString(2, p.numeroIdentificacion());
                ps.setString(3, p.razonSocial());
                ps.setString(4, p.nombreComercial());
                ps.setString(5, p.direccion());
                ps.setString(6, p.telefono());
                ps.setString(7, p.email());
                ps.setString(8, p.tipoContribuyente());
                ps.setInt(9, p.diasCredito());
                ps.setBoolean(10, p.aplicaRetencionIva());
                ps.setBoolean(11, p.aplicaRetencionRenta());
                ps.setBoolean(12, p.activo());
                ps.setString(13, p.nota());
                ps.setLong(14, p.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void asignar(PreparedStatement ps, Proveedor p) throws SQLException {
        ps.setLong(1, p.empresaId());
        ps.setString(2, p.tipoIdentificacion());
        ps.setString(3, p.numeroIdentificacion());
        ps.setString(4, p.razonSocial());
        ps.setString(5, p.nombreComercial());
        ps.setString(6, p.direccion());
        ps.setString(7, p.telefono());
        ps.setString(8, p.email());
        ps.setString(9, p.tipoContribuyente());
        ps.setInt(10, p.diasCredito());
        ps.setBoolean(11, p.aplicaRetencionIva());
        ps.setBoolean(12, p.aplicaRetencionRenta());
        ps.setBoolean(13, p.activo());
        ps.setString(14, p.nota());
    }

    // ------------------------------------------------------------------ producto-proveedor

    public List<ProductoProveedor> productosDe(long proveedorId) {
        return Bd.consulta(c -> {
            String sql = "SELECT pp.id, pp.producto_id, p.nombre AS producto, pp.proveedor_id, "
                    + "pr.razon_social AS proveedor, pp.codigo_proveedor, pp.costo_ultimo, pp.plazo_dias, "
                    + "pp.es_preferido FROM producto_proveedor pp JOIN producto p ON p.id = pp.producto_id "
                    + "JOIN proveedor pr ON pr.id = pp.proveedor_id WHERE pp.proveedor_id = ? ORDER BY p.nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, proveedorId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ProductoProveedor> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapearPP(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public void guardarProductoProveedor(ProductoProveedor pp) {
        Bd.transaccion(c -> {
            String sql = "INSERT INTO producto_proveedor (producto_id, proveedor_id, codigo_proveedor, costo_ultimo, "
                    + "plazo_dias, es_preferido) VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (producto_id, proveedor_id) DO UPDATE SET codigo_proveedor = EXCLUDED.codigo_proveedor, "
                    + "costo_ultimo = EXCLUDED.costo_ultimo, plazo_dias = EXCLUDED.plazo_dias, "
                    + "es_preferido = EXCLUDED.es_preferido";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, pp.productoId());
                ps.setLong(2, pp.proveedorId());
                ps.setString(3, pp.codigoProveedor());
                ps.setBigDecimal(4, pp.costoUltimo());
                if (pp.plazoDias() == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, pp.plazoDias());
                }
                ps.setBoolean(6, pp.esPreferido());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void eliminarProductoProveedor(long id) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM producto_proveedor WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private ProductoProveedor mapearPP(ResultSet rs) throws SQLException {
        return new ProductoProveedor(rs.getLong("id"), rs.getLong("producto_id"), rs.getString("producto"),
                rs.getLong("proveedor_id"), rs.getString("proveedor"), rs.getString("codigo_proveedor"),
                rs.getBigDecimal("costo_ultimo"), (Integer) rs.getObject("plazo_dias"), rs.getBoolean("es_preferido"));
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        return new Proveedor(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("tipo_identificacion"),
                rs.getString("numero_identificacion"), rs.getString("razon_social"), rs.getString("nombre_comercial"),
                rs.getString("direccion"), rs.getString("telefono"), rs.getString("email"),
                rs.getString("tipo_contribuyente"), rs.getInt("dias_credito"), rs.getBoolean("aplica_retencion_iva"),
                rs.getBoolean("aplica_retencion_renta"), rs.getBoolean("activo"), rs.getString("nota"));
    }
}
