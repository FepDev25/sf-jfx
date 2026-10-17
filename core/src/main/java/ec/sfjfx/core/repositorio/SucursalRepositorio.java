package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Sucursal;

public final class SucursalRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, codigo, nombre, direccion, telefono, es_matriz, activo";

    public List<Sucursal> porEmpresa(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM sucursal WHERE empresa_id = ? "
                    + "ORDER BY activo DESC, es_matriz DESC, nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    return mapearLista(rs);
                }
            }
        });
    }

    /** Sucursales del usuario segun usuario_sucursal; si no tiene, todas las de la empresa. */
    public List<Sucursal> porUsuario(long usuarioId, long empresaId) {
        List<Sucursal> asignadas = Bd.consulta(c -> {
            String sql = "SELECT s.id, s.empresa_id, s.codigo, s.nombre, s.direccion, s.telefono, "
                    + "s.es_matriz, s.activo FROM sucursal s "
                    + "JOIN usuario_sucursal us ON us.sucursal_id = s.id "
                    + "WHERE us.usuario_id = ? AND s.activo = TRUE ORDER BY s.es_matriz DESC, s.nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    return mapearLista(rs);
                }
            }
        });
        return asignadas.isEmpty() ? porEmpresa(empresaId) : asignadas;
    }

    public Optional<Sucursal> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM sucursal WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public long crear(Sucursal s) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO sucursal (empresa_id, codigo, nombre, direccion, telefono, es_matriz, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, s);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Sucursal s) {
        Bd.transaccion(c -> {
            String sql = "UPDATE sucursal SET codigo = ?, nombre = ?, direccion = ?, telefono = ?, "
                    + "es_matriz = ?, activo = ?, actualizado_en = now() WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, s.codigo());
                ps.setString(2, s.nombre());
                ps.setString(3, s.direccion());
                ps.setString(4, s.telefono());
                ps.setBoolean(5, s.esMatriz());
                ps.setBoolean(6, s.activo());
                ps.setLong(7, s.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void asignar(PreparedStatement ps, Sucursal s) throws SQLException {
        ps.setLong(1, s.empresaId());
        ps.setString(2, s.codigo());
        ps.setString(3, s.nombre());
        ps.setString(4, s.direccion());
        ps.setString(5, s.telefono());
        ps.setBoolean(6, s.esMatriz());
        ps.setBoolean(7, s.activo());
    }

    private List<Sucursal> mapearLista(ResultSet rs) throws SQLException {
        List<Sucursal> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    private Sucursal mapear(ResultSet rs) throws SQLException {
        return new Sucursal(
                rs.getLong("id"),
                rs.getLong("empresa_id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("direccion"),
                rs.getString("telefono"),
                rs.getBoolean("es_matriz"),
                rs.getBoolean("activo"));
    }
}
