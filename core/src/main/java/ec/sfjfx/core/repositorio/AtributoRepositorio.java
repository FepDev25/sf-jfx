package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.AtributoDefinicion;
import ec.sfjfx.core.modelo.TipoDatoAtributo;

public final class AtributoRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, categoria_id, codigo, nombre, tipo_dato, opciones, obligatorio, orden, activo";

    public List<AtributoDefinicion> listar(long empresaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM atributo_definicion WHERE empresa_id = ? ORDER BY orden, nombre")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    return mapearLista(rs);
                }
            }
        });
    }

    /** Definiciones aplicables a una categoria: las globales (sin categoria) mas las de la categoria. */
    public List<AtributoDefinicion> aplicables(long empresaId, Long categoriaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM atributo_definicion WHERE empresa_id = ? AND activo = TRUE "
                    + "AND (categoria_id IS NULL OR categoria_id = ?) ORDER BY orden, nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setObject(2, categoriaId);
                try (ResultSet rs = ps.executeQuery()) {
                    return mapearLista(rs);
                }
            }
        });
    }

    public Optional<AtributoDefinicion> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM atributo_definicion WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean codigoExiste(long empresaId, String codigo, Long excluirId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM atributo_definicion WHERE empresa_id = ? AND lower(codigo) = lower(?) "
                    + "AND (? IS NULL OR id <> ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, codigo);
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

    public long crear(AtributoDefinicion a) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO atributo_definicion (empresa_id, categoria_id, codigo, nombre, tipo_dato, "
                    + "opciones, obligatorio, orden, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, a);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(AtributoDefinicion a) {
        Bd.transaccion(c -> {
            String sql = "UPDATE atributo_definicion SET categoria_id = ?, codigo = ?, nombre = ?, tipo_dato = ?, "
                    + "opciones = ?, obligatorio = ?, orden = ?, activo = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, a.categoriaId());
                ps.setString(2, a.codigo());
                ps.setString(3, a.nombre());
                ps.setString(4, a.tipoDato().name());
                ps.setString(5, a.opciones());
                ps.setBoolean(6, a.obligatorio());
                ps.setInt(7, a.orden());
                ps.setBoolean(8, a.activo());
                ps.setLong(9, a.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void asignar(PreparedStatement ps, AtributoDefinicion a) throws SQLException {
        ps.setLong(1, a.empresaId());
        ps.setObject(2, a.categoriaId());
        ps.setString(3, a.codigo());
        ps.setString(4, a.nombre());
        ps.setString(5, a.tipoDato().name());
        ps.setString(6, a.opciones());
        ps.setBoolean(7, a.obligatorio());
        ps.setInt(8, a.orden());
        ps.setBoolean(9, a.activo());
    }

    private List<AtributoDefinicion> mapearLista(ResultSet rs) throws SQLException {
        List<AtributoDefinicion> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    private AtributoDefinicion mapear(ResultSet rs) throws SQLException {
        return new AtributoDefinicion(rs.getLong("id"), rs.getLong("empresa_id"),
                (Long) rs.getObject("categoria_id"), rs.getString("codigo"), rs.getString("nombre"),
                TipoDatoAtributo.valueOf(rs.getString("tipo_dato")), rs.getString("opciones"),
                rs.getBoolean("obligatorio"), rs.getInt("orden"), rs.getBoolean("activo"));
    }
}
