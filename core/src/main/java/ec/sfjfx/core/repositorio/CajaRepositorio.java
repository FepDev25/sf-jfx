package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Caja;

public final class CajaRepositorio {

    private static final String COLUMNAS = "id, sucursal_id, codigo, nombre, estado, activo";

    public List<Caja> porSucursal(long sucursalId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM caja WHERE sucursal_id = ? "
                    + "ORDER BY activo DESC, nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, sucursalId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Caja> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    /** Cajas activas de la empresa (para vistas de administracion). */
    public List<Caja> porEmpresa(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT c.id, c.sucursal_id, c.codigo, c.nombre, c.estado, c.activo "
                    + "FROM caja c JOIN sucursal s ON s.id = c.sucursal_id "
                    + "WHERE s.empresa_id = ? ORDER BY c.activo DESC, c.nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Caja> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Caja> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM caja WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public long crear(Caja k) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO caja (sucursal_id, codigo, nombre, estado, activo) "
                    + "VALUES (?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, k.sucursalId());
                ps.setString(2, k.codigo());
                ps.setString(3, k.nombre());
                ps.setString(4, k.estado());
                ps.setBoolean(5, k.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Caja k) {
        Bd.transaccion(c -> {
            String sql = "UPDATE caja SET codigo = ?, nombre = ?, estado = ?, activo = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, k.codigo());
                ps.setString(2, k.nombre());
                ps.setString(3, k.estado());
                ps.setBoolean(4, k.activo());
                ps.setLong(5, k.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Caja mapear(ResultSet rs) throws SQLException {
        return new Caja(
                rs.getLong("id"),
                rs.getLong("sucursal_id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("estado"),
                rs.getBoolean("activo"));
    }
}
