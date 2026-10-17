package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.MovimientoSolicitud;
import ec.sfjfx.core.modelo.MovimientoVista;
import ec.sfjfx.core.modelo.StockItem;

public final class InventarioRepositorio {

    private static volatile Map<String, Long> tiposMovimiento;

    // ------------------------------------------------------------------ tipos de movimiento

    public long tipoMovimientoId(Connection c, String codigo) throws SQLException {
        Map<String, Long> mapa = tiposMovimiento;
        if (mapa == null) {
            mapa = new HashMap<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT codigo, id FROM tipo_movimiento_inventario");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getString("codigo"), rs.getLong("id"));
                }
            }
            tiposMovimiento = mapa;
        }
        Long id = mapa.get(codigo);
        if (id == null) {
            throw new RepositorioExcepcion("Tipo de movimiento desconocido: " + codigo, null);
        }
        return id;
    }

    // ------------------------------------------------------------------ primitivo de movimiento

    /** Aplica un movimiento: actualiza stock (upsert) e inserta la fila del kardex. */
    public void aplicar(Connection c, MovimientoSolicitud m, BigDecimal costoReferencia) throws SQLException {
        BigDecimal saldoCantidad = actualizarStock(c, m.bodegaId(), m.productoId(), m.loteId(), m.cantidad());

        String sql = "INSERT INTO movimiento_inventario (empresa_id, sucursal_id, bodega_id, producto_id, lote_id, "
                + "serie_id, tipo_movimiento_id, cantidad, costo_unitario, saldo_cantidad, saldo_costo, "
                + "referencia_tipo, referencia_id, usuario_id, nota) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, m.empresaId());
            ps.setLong(2, m.sucursalId());
            ps.setLong(3, m.bodegaId());
            ps.setLong(4, m.productoId());
            ps.setObject(5, m.loteId());
            ps.setObject(6, m.serieId());
            ps.setLong(7, tipoMovimientoId(c, m.tipoMovimiento()));
            ps.setBigDecimal(8, m.cantidad());
            ps.setBigDecimal(9, nz(m.costoUnitario()));
            ps.setBigDecimal(10, saldoCantidad);
            ps.setBigDecimal(11, saldoCantidad.multiply(nz(costoReferencia)));
            ps.setString(12, m.referenciaTipo());
            ps.setObject(13, m.referenciaId());
            ps.setObject(14, m.usuarioId());
            ps.setString(15, m.nota());
            ps.executeUpdate();
        }
    }

    private BigDecimal actualizarStock(Connection c, long bodegaId, long productoId, Long loteId, BigDecimal delta)
            throws SQLException {
        String select = "SELECT id, cantidad FROM stock WHERE bodega_id = ? AND producto_id = ? "
                + "AND COALESCE(lote_id, 0) = COALESCE(?, 0) FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(select)) {
            ps.setLong(1, bodegaId);
            ps.setLong(2, productoId);
            ps.setObject(3, loteId, Types.BIGINT);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    BigDecimal nueva = rs.getBigDecimal("cantidad").add(delta);
                    try (PreparedStatement up = c.prepareStatement(
                            "UPDATE stock SET cantidad = ?, actualizado_en = now() WHERE id = ?")) {
                        up.setBigDecimal(1, nueva);
                        up.setLong(2, id);
                        up.executeUpdate();
                    }
                    return nueva;
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO stock (bodega_id, producto_id, lote_id, cantidad, cantidad_reservada) "
                + "VALUES (?, ?, ?, ?, 0)")) {
            ps.setLong(1, bodegaId);
            ps.setLong(2, productoId);
            ps.setObject(3, loteId, Types.BIGINT);
            ps.setBigDecimal(4, delta);
            ps.executeUpdate();
        }
        return delta;
    }

    // ------------------------------------------------------------------ costo

    public BigDecimal stockTotal(Connection c, long productoId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(cantidad), 0) FROM stock WHERE producto_id = ?")) {
            ps.setLong(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    public BigDecimal costoPromedio(Connection c, long productoId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT costo_promedio FROM producto WHERE id = ?")) {
            ps.setLong(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return nz(rs.getBigDecimal(1));
            }
        }
    }

    public long sucursalDeBodega(Connection c, long bodegaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT sucursal_id FROM bodega WHERE id = ?")) {
            ps.setLong(1, bodegaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void actualizarCosto(Connection c, long productoId, BigDecimal promedio, BigDecimal ultimo)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE producto SET costo_promedio = ?, costo = ?, actualizado_en = now() WHERE id = ?")) {
            ps.setBigDecimal(1, promedio);
            ps.setBigDecimal(2, ultimo);
            ps.setLong(3, productoId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ consultas

    public List<StockItem> stock(long empresaId, Long bodegaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT s.producto_id, p.nombre, p.codigo, s.bodega_id, b.nombre AS bodega, "
                    + "s.lote_id, l.codigo AS lote, s.cantidad, s.cantidad_reservada, p.costo_promedio "
                    + "FROM stock s "
                    + "JOIN producto p ON p.id = s.producto_id "
                    + "JOIN bodega b ON b.id = s.bodega_id "
                    + "JOIN sucursal su ON su.id = b.sucursal_id "
                    + "LEFT JOIN lote l ON l.id = s.lote_id "
                    + "WHERE su.empresa_id = ? AND (? IS NULL OR s.bodega_id = ?) "
                    + "AND (? IS NULL OR p.nombre ILIKE ? OR p.codigo ILIKE ?) "
                    + "AND s.cantidad <> 0 ORDER BY p.nombre, b.nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setObject(2, bodegaId, Types.BIGINT);
                ps.setObject(3, bodegaId, Types.BIGINT);
                ps.setString(4, patron);
                ps.setString(5, patron);
                ps.setString(6, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<StockItem> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapearStock(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public List<StockItem> stockPorProducto(long productoId) {
        return Bd.consulta(c -> {
            String sql = "SELECT s.producto_id, p.nombre, p.codigo, s.bodega_id, b.nombre AS bodega, "
                    + "s.lote_id, l.codigo AS lote, s.cantidad, s.cantidad_reservada, p.costo_promedio "
                    + "FROM stock s JOIN producto p ON p.id = s.producto_id JOIN bodega b ON b.id = s.bodega_id "
                    + "LEFT JOIN lote l ON l.id = s.lote_id WHERE s.producto_id = ? ORDER BY b.nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, productoId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<StockItem> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapearStock(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public BigDecimal valorInventario(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT COALESCE(SUM(s.cantidad * p.costo_promedio), 0) FROM stock s "
                    + "JOIN producto p ON p.id = s.producto_id "
                    + "JOIN bodega b ON b.id = s.bodega_id JOIN sucursal su ON su.id = b.sucursal_id "
                    + "WHERE su.empresa_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getBigDecimal(1);
                }
            }
        });
    }

    public List<MovimientoVista> kardex(long productoId, Long bodegaId, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT m.id, m.creado_en, b.nombre AS bodega, p.nombre AS producto, "
                    + "t.nombre AS tipo, m.cantidad, m.costo_unitario, m.saldo_cantidad, "
                    + "m.referencia_tipo, m.referencia_id, u.nombre_usuario, m.nota "
                    + "FROM movimiento_inventario m "
                    + "JOIN bodega b ON b.id = m.bodega_id "
                    + "JOIN producto p ON p.id = m.producto_id "
                    + "JOIN tipo_movimiento_inventario t ON t.id = m.tipo_movimiento_id "
                    + "LEFT JOIN usuario u ON u.id = m.usuario_id "
                    + "WHERE m.producto_id = ? AND (? IS NULL OR m.bodega_id = ?) "
                    + "ORDER BY m.creado_en DESC, m.id DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, productoId);
                ps.setObject(2, bodegaId, Types.BIGINT);
                ps.setObject(3, bodegaId, Types.BIGINT);
                ps.setInt(4, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<MovimientoVista> lista = new ArrayList<>();
                    while (rs.next()) {
                        String ref = rs.getString("referencia_tipo");
                        Long refId = (Long) rs.getObject("referencia_id");
                        lista.add(new MovimientoVista(
                                rs.getLong("id"),
                                rs.getObject("creado_en", LocalDateTime.class),
                                rs.getString("bodega"),
                                rs.getString("producto"),
                                rs.getString("tipo"),
                                rs.getBigDecimal("cantidad"),
                                rs.getBigDecimal("costo_unitario"),
                                rs.getBigDecimal("saldo_cantidad"),
                                ref == null ? "" : ref + (refId == null ? "" : " #" + refId),
                                rs.getString("nombre_usuario"),
                                rs.getString("nota")));
                    }
                    return lista;
                }
            }
        });
    }

    private StockItem mapearStock(ResultSet rs) throws SQLException {
        return new StockItem(
                rs.getLong("producto_id"),
                rs.getString("nombre"),
                rs.getString("codigo"),
                rs.getLong("bodega_id"),
                rs.getString("bodega"),
                (Long) rs.getObject("lote_id"),
                rs.getString("lote"),
                rs.getBigDecimal("cantidad"),
                rs.getBigDecimal("cantidad_reservada"),
                rs.getBigDecimal("costo_promedio"));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
