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
import ec.sfjfx.core.modelo.Lote;

public final class LoteRepositorio {

    public List<Lote> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT l.id, l.producto_id, p.nombre AS producto, l.codigo, l.fecha_fabricacion, "
                    + "l.fecha_vencimiento, l.activo FROM lote l JOIN producto p ON p.id = l.producto_id "
                    + "WHERE p.empresa_id = ? AND (? IS NULL OR l.codigo ILIKE ? OR p.nombre ILIKE ?) "
                    + "ORDER BY l.fecha_vencimiento NULLS LAST, l.codigo LIMIT 500";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                ps.setString(4, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Lote> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public List<Lote> porProducto(long productoId) {
        return Bd.consulta(c -> {
            String sql = "SELECT l.id, l.producto_id, p.nombre AS producto, l.codigo, l.fecha_fabricacion, "
                    + "l.fecha_vencimiento, l.activo FROM lote l JOIN producto p ON p.id = l.producto_id "
                    + "WHERE l.producto_id = ? ORDER BY l.fecha_vencimiento NULLS LAST";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, productoId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Lote> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Lote> porId(long id) {
        return Bd.consulta(c -> porId(c, id));
    }

    public Optional<Lote> porId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT l.id, l.producto_id, p.nombre AS producto, l.codigo, l.fecha_fabricacion, "
                + "l.fecha_vencimiento, l.activo FROM lote l JOIN producto p ON p.id = l.producto_id WHERE l.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public long crear(Lote lote) {
        return Bd.transaccion(c -> buscarOCrear(c, lote.productoId(), lote.codigo(), lote.fechaFabricacion(),
                lote.fechaVencimiento()));
    }

    /** Busca un lote por (producto, codigo) o lo crea. Devuelve su id. */
    public long buscarOCrear(Connection c, long productoId, String codigo, LocalDate fabricacion, LocalDate vencimiento)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM lote WHERE producto_id = ? AND codigo = ?")) {
            ps.setLong(1, productoId);
            ps.setString(2, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO lote (producto_id, codigo, fecha_fabricacion, fecha_vencimiento) "
                + "VALUES (?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, productoId);
            ps.setString(2, codigo);
            ps.setObject(3, fabricacion, Types.DATE);
            ps.setObject(4, vencimiento, Types.DATE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private Lote mapear(ResultSet rs) throws SQLException {
        return new Lote(rs.getLong("id"), rs.getLong("producto_id"), rs.getString("producto"),
                rs.getString("codigo"), rs.getObject("fecha_fabricacion", LocalDate.class),
                rs.getObject("fecha_vencimiento", LocalDate.class), rs.getBoolean("activo"));
    }
}
