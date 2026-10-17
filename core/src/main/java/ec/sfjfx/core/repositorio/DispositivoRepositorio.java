package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Dispositivo;

public final class DispositivoRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, sucursal_id, caja_id, tipo, nombre, configuracion::text AS configuracion, activo";

    public List<Dispositivo> porEmpresa(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM dispositivo WHERE empresa_id = ? ORDER BY tipo, nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Dispositivo> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Dispositivo> porId(long id) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM dispositivo WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public long crear(Dispositivo d) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO dispositivo (empresa_id, sucursal_id, caja_id, tipo, nombre, configuracion, activo) "
                    + "VALUES (?, ?, ?, ?, ?, CAST(? AS JSONB), ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, d.empresaId());
                ps.setObject(2, d.sucursalId(), Types.BIGINT);
                ps.setObject(3, d.cajaId(), Types.BIGINT);
                ps.setString(4, d.tipo());
                ps.setString(5, d.nombre());
                ps.setString(6, json(d.configuracion()));
                ps.setBoolean(7, d.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Dispositivo d) {
        Bd.transaccion(c -> {
            String sql = "UPDATE dispositivo SET sucursal_id = ?, caja_id = ?, tipo = ?, nombre = ?, "
                    + "configuracion = CAST(? AS JSONB), activo = ? WHERE id = ? AND empresa_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, d.sucursalId(), Types.BIGINT);
                ps.setObject(2, d.cajaId(), Types.BIGINT);
                ps.setString(3, d.tipo());
                ps.setString(4, d.nombre());
                ps.setString(5, json(d.configuracion()));
                ps.setBoolean(6, d.activo());
                ps.setLong(7, d.id());
                ps.setLong(8, d.empresaId());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void eliminar(long id, long empresaId) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM dispositivo WHERE id = ? AND empresa_id = ?")) {
                ps.setLong(1, id);
                ps.setLong(2, empresaId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static String json(String configuracion) {
        return configuracion == null || configuracion.isBlank() ? "{}" : configuracion.trim();
    }

    private static Dispositivo mapear(ResultSet rs) throws SQLException {
        return new Dispositivo(rs.getLong("id"), rs.getLong("empresa_id"),
                rs.getObject("sucursal_id", Long.class), rs.getObject("caja_id", Long.class),
                rs.getString("tipo"), rs.getString("nombre"), rs.getString("configuracion"),
                rs.getBoolean("activo"));
    }
}
