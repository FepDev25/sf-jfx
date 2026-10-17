package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Cupon;

public final class CuponRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, codigo, tipo, valor, vigente_desde, vigente_hasta, usos_maximos, usos_actuales, activo";

    public List<Cupon> listar(long empresaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM cupon WHERE empresa_id = ? ORDER BY id DESC LIMIT 500")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Cupon> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Cupon> porCodigo(long empresaId, String codigo) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM cupon WHERE empresa_id = ? AND upper(codigo) = upper(?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, codigo);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean codigoExiste(long empresaId, String codigo, Long excluirId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM cupon WHERE empresa_id = ? AND upper(codigo) = upper(?) AND (? IS NULL OR id <> ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, codigo);
                if (excluirId == null) {
                    ps.setNull(3, Types.BIGINT);
                    ps.setNull(4, Types.BIGINT);
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

    public long crear(Cupon c) {
        return Bd.transaccion(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO cupon (empresa_id, codigo, tipo, valor, vigente_desde, vigente_hasta, usos_maximos, "
                    + "usos_actuales, activo) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?) RETURNING id")) {
                ps.setLong(1, c.empresaId());
                ps.setString(2, c.codigo());
                ps.setString(3, c.tipo());
                ps.setBigDecimal(4, c.valor());
                ps.setObject(5, c.vigenteDesde());
                ps.setObject(6, c.vigenteHasta());
                ps.setObject(7, c.usosMaximos(), Types.INTEGER);
                ps.setBoolean(8, c.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Cupon c) {
        Bd.transaccion(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE cupon SET codigo = ?, tipo = ?, valor = ?, vigente_desde = ?, vigente_hasta = ?, "
                    + "usos_maximos = ?, activo = ? WHERE id = ?")) {
                ps.setString(1, c.codigo());
                ps.setString(2, c.tipo());
                ps.setBigDecimal(3, c.valor());
                ps.setObject(4, c.vigenteDesde());
                ps.setObject(5, c.vigenteHasta());
                ps.setObject(6, c.usosMaximos(), Types.INTEGER);
                ps.setBoolean(7, c.activo());
                ps.setLong(8, c.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void incrementarUso(long id) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE cupon SET usos_actuales = usos_actuales + 1 WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Cupon mapear(ResultSet rs) throws SQLException {
        return new Cupon(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("codigo"), rs.getString("tipo"),
                rs.getBigDecimal("valor"), rs.getObject("vigente_desde", LocalDate.class),
                rs.getObject("vigente_hasta", LocalDate.class), (Integer) rs.getObject("usos_maximos"),
                rs.getInt("usos_actuales"), rs.getBoolean("activo"));
    }
}
