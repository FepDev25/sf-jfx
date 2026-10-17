package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.TipoListaPrecio;

/** Consulta de precios por lista. */
public final class PrecioRepositorio {

    public Optional<ListaPrecio> listaDefault(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT id, empresa_id, nombre, tipo, moneda, incluye_iva, es_default, activo "
                    + "FROM lista_precio WHERE empresa_id = ? AND activo = TRUE "
                    + "ORDER BY es_default DESC, (tipo = 'PUBLICO') DESC, id LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new ListaPrecio(rs.getLong("id"), rs.getLong("empresa_id"),
                            rs.getString("nombre"), TipoListaPrecio.valueOf(rs.getString("tipo")),
                            rs.getString("moneda"), rs.getBoolean("incluye_iva"), rs.getBoolean("es_default"),
                            rs.getBoolean("activo")));
                }
            }
        });
    }

    public Optional<BigDecimal> precioVigente(long productoId, long listaPrecioId) {
        return Bd.consulta(c -> {
            String sql = "SELECT precio FROM precio_producto WHERE producto_id = ? AND lista_precio_id = ? "
                    + "AND (vigente_desde IS NULL OR vigente_desde <= CURRENT_DATE) "
                    + "AND (vigente_hasta IS NULL OR vigente_hasta >= CURRENT_DATE) "
                    + "ORDER BY vigente_desde DESC NULLS LAST LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, productoId);
                ps.setLong(2, listaPrecioId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty();
                }
            }
        });
    }

    public Map<Long, BigDecimal> mapa(long listaPrecioId) {
        return Bd.consulta(c -> {
            String sql = "SELECT producto_id, precio FROM precio_producto WHERE lista_precio_id = ? "
                    + "AND (vigente_hasta IS NULL OR vigente_hasta >= CURRENT_DATE)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, listaPrecioId);
                try (ResultSet rs = ps.executeQuery()) {
                    Map<Long, BigDecimal> mapa = new HashMap<>();
                    while (rs.next()) {
                        mapa.put(rs.getLong("producto_id"), rs.getBigDecimal("precio"));
                    }
                    return mapa;
                }
            }
        });
    }
}
