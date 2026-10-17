package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Serie;

public final class SerieRepositorio {

    public List<Serie> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT s.id, s.producto_id, p.nombre AS producto, s.numero_serie, s.estado, "
                    + "s.bodega_id, b.nombre AS bodega, s.lote_id, s.venta_id "
                    + "FROM serie s JOIN producto p ON p.id = s.producto_id "
                    + "LEFT JOIN bodega b ON b.id = s.bodega_id "
                    + "WHERE p.empresa_id = ? AND (? IS NULL OR s.numero_serie ILIKE ? OR p.nombre ILIKE ?) "
                    + "ORDER BY s.numero_serie LIMIT 500";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Serie> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public long buscarOCrear(Connection c, long productoId, String numeroSerie, Long bodegaId, Long loteId)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM serie WHERE numero_serie = ?")) {
            ps.setString(1, numeroSerie);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO serie (producto_id, numero_serie, estado, bodega_id, lote_id) "
                + "VALUES (?, ?, 'DISPONIBLE', ?, ?) RETURNING id")) {
            ps.setLong(1, productoId);
            ps.setString(2, numeroSerie);
            ps.setObject(3, bodegaId, Types.BIGINT);
            ps.setObject(4, loteId, Types.BIGINT);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public List<Serie> porProducto(long productoId) {
        return Bd.consulta(c -> {
            String sql = "SELECT s.id, s.producto_id, p.nombre AS producto, s.numero_serie, s.estado, "
                    + "s.bodega_id, b.nombre AS bodega, s.lote_id, s.venta_id "
                    + "FROM serie s JOIN producto p ON p.id = s.producto_id "
                    + "LEFT JOIN bodega b ON b.id = s.bodega_id WHERE s.producto_id = ? ORDER BY s.numero_serie";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, productoId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Serie> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public void cambiarEstado(long serieId, String estado) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE serie SET estado = ? WHERE id = ?")) {
                ps.setString(1, estado);
                ps.setLong(2, serieId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Serie mapear(ResultSet rs) throws SQLException {
        return new Serie(rs.getLong("id"), rs.getLong("producto_id"), rs.getString("producto"),
                rs.getString("numero_serie"), rs.getString("estado"), (Long) rs.getObject("bodega_id"),
                rs.getString("bodega"), (Long) rs.getObject("lote_id"), (Long) rs.getObject("venta_id"));
    }
}
