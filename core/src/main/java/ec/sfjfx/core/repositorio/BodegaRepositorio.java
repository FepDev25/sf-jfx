package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Bodega;

public final class BodegaRepositorio {

    private static final String COLUMNAS = "id, sucursal_id, codigo, nombre, tipo, direccion, activo";

    public List<Bodega> porSucursal(long sucursalId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM bodega WHERE sucursal_id = ? ORDER BY activo DESC, nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, sucursalId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Bodega> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public List<Bodega> porEmpresa(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT b.id, b.sucursal_id, b.codigo, b.nombre, b.tipo, b.direccion, b.activo "
                    + "FROM bodega b JOIN sucursal s ON s.id = b.sucursal_id "
                    + "WHERE s.empresa_id = ? ORDER BY b.activo DESC, b.nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Bodega> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Bodega> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM bodega WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    /** Bodega de venta de una sucursal (o la primera activa). */
    public Optional<Bodega> bodegaVentaDeSucursal(long sucursalId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM bodega WHERE sucursal_id = ? AND activo = TRUE "
                    + "ORDER BY (tipo = 'VENTA') DESC, id LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, sucursalId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public long crear(Bodega b) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO bodega (sucursal_id, codigo, nombre, tipo, direccion, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, b.sucursalId());
                ps.setString(2, b.codigo());
                ps.setString(3, b.nombre());
                ps.setString(4, b.tipo());
                ps.setString(5, b.direccion());
                ps.setBoolean(6, b.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Bodega b) {
        Bd.transaccion(c -> {
            String sql = "UPDATE bodega SET codigo = ?, nombre = ?, tipo = ?, direccion = ?, activo = ? "
                    + "WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, b.codigo());
                ps.setString(2, b.nombre());
                ps.setString(3, b.tipo());
                ps.setString(4, b.direccion());
                ps.setBoolean(5, b.activo());
                ps.setLong(6, b.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Bodega mapear(ResultSet rs) throws SQLException {
        return new Bodega(
                rs.getLong("id"),
                rs.getLong("sucursal_id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("tipo"),
                rs.getString("direccion"),
                rs.getBoolean("activo"));
    }
}
