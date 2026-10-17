package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.HistorialPrecio;

public final class HistorialPrecioRepositorio {

    public void registrar(Connection c, long empresaId, long productoId, Long listaPrecioId, String tipo,
                          java.math.BigDecimal anterior, java.math.BigDecimal nuevo, Long sucursalId,
                          Long usuarioId) throws SQLException {
        String sql = "INSERT INTO historial_precio (empresa_id, producto_id, lista_precio_id, tipo, "
                + "valor_anterior, valor_nuevo, sucursal_id, usuario_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, productoId);
            ps.setObject(3, listaPrecioId);
            ps.setString(4, tipo);
            ps.setBigDecimal(5, anterior);
            ps.setBigDecimal(6, nuevo);
            ps.setObject(7, sucursalId);
            ps.setObject(8, usuarioId);
            ps.executeUpdate();
        }
    }

    public List<HistorialPrecio> porProducto(long productoId, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT h.id, h.producto_id, h.lista_precio_id, h.tipo, h.valor_anterior, "
                    + "h.valor_nuevo, h.creado_en, u.nombre_usuario "
                    + "FROM historial_precio h LEFT JOIN usuario u ON u.id = h.usuario_id "
                    + "WHERE h.producto_id = ? ORDER BY h.creado_en DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, productoId);
                ps.setInt(2, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<HistorialPrecio> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new HistorialPrecio(rs.getLong("id"), rs.getLong("producto_id"),
                                (Long) rs.getObject("lista_precio_id"), rs.getString("tipo"),
                                rs.getBigDecimal("valor_anterior"), rs.getBigDecimal("valor_nuevo"),
                                rs.getObject("creado_en", LocalDateTime.class), rs.getString("nombre_usuario")));
                    }
                    return lista;
                }
            }
        });
    }
}
