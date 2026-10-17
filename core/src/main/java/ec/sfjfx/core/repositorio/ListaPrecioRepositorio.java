package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.ListaPrecio;
import ec.sfjfx.core.modelo.TipoListaPrecio;

public final class ListaPrecioRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, nombre, tipo, moneda, incluye_iva, es_default, activo";

    public List<ListaPrecio> listar(long empresaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM lista_precio WHERE empresa_id = ? ORDER BY es_default DESC, nombre")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ListaPrecio> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public List<ListaPrecio> activas(long empresaId) {
        return listar(empresaId).stream().filter(ListaPrecio::activo).toList();
    }

    public Optional<ListaPrecio> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM lista_precio WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean nombreExiste(long empresaId, String nombre, Long excluirId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM lista_precio WHERE empresa_id = ? AND lower(nombre) = lower(?) "
                    + "AND (? IS NULL OR id <> ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, nombre);
                if (excluirId == null) {
                    ps.setNull(3, java.sql.Types.BIGINT);
                    ps.setNull(4, java.sql.Types.BIGINT);
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

    public long crear(ListaPrecio l) {
        return Bd.transaccion(c -> {
            if (l.esDefault()) {
                quitarDefault(c, l.empresaId());
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO lista_precio (empresa_id, nombre, tipo, moneda, incluye_iva, es_default, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
                ps.setLong(1, l.empresaId());
                ps.setString(2, l.nombre());
                ps.setString(3, l.tipo().name());
                ps.setString(4, l.moneda());
                ps.setBoolean(5, l.incluyeIva());
                ps.setBoolean(6, l.esDefault());
                ps.setBoolean(7, l.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(ListaPrecio l) {
        Bd.transaccion(c -> {
            if (l.esDefault()) {
                quitarDefault(c, l.empresaId());
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE lista_precio SET nombre = ?, tipo = ?, moneda = ?, incluye_iva = ?, "
                    + "es_default = ?, activo = ? WHERE id = ?")) {
                ps.setString(1, l.nombre());
                ps.setString(2, l.tipo().name());
                ps.setString(3, l.moneda());
                ps.setBoolean(4, l.incluyeIva());
                ps.setBoolean(5, l.esDefault());
                ps.setBoolean(6, l.activo());
                ps.setLong(7, l.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void quitarDefault(java.sql.Connection c, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE lista_precio SET es_default = FALSE WHERE empresa_id = ?")) {
            ps.setLong(1, empresaId);
            ps.executeUpdate();
        }
    }

    private ListaPrecio mapear(ResultSet rs) throws SQLException {
        return new ListaPrecio(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("nombre"),
                TipoListaPrecio.valueOf(rs.getString("tipo")), rs.getString("moneda"),
                rs.getBoolean("incluye_iva"), rs.getBoolean("es_default"), rs.getBoolean("activo"));
    }
}
