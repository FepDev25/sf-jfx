package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.TipoUnidad;
import ec.sfjfx.core.modelo.UnidadMedida;

public final class UnidadMedidaRepositorio {

    private static final String COLUMNAS =
            "id, codigo, nombre, abreviatura, tipo, permite_decimales, activo";

    public List<UnidadMedida> listar() {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM unidad_medida ORDER BY tipo, nombre");
                 ResultSet rs = ps.executeQuery()) {
                List<UnidadMedida> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
                return lista;
            }
        });
    }

    public Optional<UnidadMedida> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM unidad_medida WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public long crear(UnidadMedida u) {
        return Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO unidad_medida (codigo, nombre, abreviatura, tipo, permite_decimales, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id")) {
                ps.setString(1, u.codigo());
                ps.setString(2, u.nombre());
                ps.setString(3, u.abreviatura());
                ps.setString(4, u.tipo().name());
                ps.setBoolean(5, u.permiteDecimales());
                ps.setBoolean(6, u.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(UnidadMedida u) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE unidad_medida SET codigo = ?, nombre = ?, abreviatura = ?, tipo = ?, "
                    + "permite_decimales = ?, activo = ? WHERE id = ?")) {
                ps.setString(1, u.codigo());
                ps.setString(2, u.nombre());
                ps.setString(3, u.abreviatura());
                ps.setString(4, u.tipo().name());
                ps.setBoolean(5, u.permiteDecimales());
                ps.setBoolean(6, u.activo());
                ps.setLong(7, u.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private UnidadMedida mapear(ResultSet rs) throws SQLException {
        return new UnidadMedida(rs.getLong("id"), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("abreviatura"), TipoUnidad.valueOf(rs.getString("tipo")),
                rs.getBoolean("permite_decimales"), rs.getBoolean("activo"));
    }
}
