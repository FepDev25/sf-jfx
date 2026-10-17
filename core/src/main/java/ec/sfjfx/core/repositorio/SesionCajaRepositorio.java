package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.SesionCaja;

public final class SesionCajaRepositorio {

    private static final String COLUMNAS =
            "s.id, s.empresa_id, s.caja_id, c.nombre AS caja, s.usuario_apertura_id, s.usuario_cierre_id, "
            + "s.fecha_apertura, s.fecha_cierre, s.monto_inicial, s.monto_esperado_efectivo, s.monto_contado_efectivo, "
            + "s.diferencia, s.estado, s.observacion";

    public Optional<SesionCaja> sesionAbierta(long cajaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM sesion_caja s JOIN caja c ON c.id = s.caja_id "
                    + "WHERE s.caja_id = ? AND s.estado = 'ABIERTA' ORDER BY s.id DESC LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, cajaId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public Optional<SesionCaja> porId(long id) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM sesion_caja s JOIN caja c ON c.id = s.caja_id WHERE s.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public long abrir(long empresaId, long cajaId, Long usuarioId, BigDecimal montoInicial) {
        return Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO sesion_caja (empresa_id, caja_id, usuario_apertura_id, monto_inicial, estado) "
                    + "VALUES (?, ?, ?, ?, 'ABIERTA') RETURNING id")) {
                ps.setLong(1, empresaId);
                ps.setLong(2, cajaId);
                ps.setObject(3, usuarioId);
                ps.setBigDecimal(4, montoInicial);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void cerrar(long sesionId, Long usuarioId, BigDecimal esperado, BigDecimal contado,
                       BigDecimal diferencia, String observacion) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE sesion_caja SET estado = 'CERRADA', fecha_cierre = now(), usuario_cierre_id = ?, "
                    + "monto_esperado_efectivo = ?, monto_contado_efectivo = ?, diferencia = ?, observacion = ? "
                    + "WHERE id = ?")) {
                ps.setObject(1, usuarioId);
                ps.setBigDecimal(2, esperado);
                ps.setBigDecimal(3, contado);
                ps.setBigDecimal(4, diferencia);
                ps.setString(5, observacion);
                ps.setLong(6, sesionId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void insertarArqueo(Connection c, long sesionId, BigDecimal denominacion, int cantidad,
                               BigDecimal subtotal) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO sesion_caja_arqueo (sesion_caja_id, denominacion, cantidad, subtotal) "
                + "VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, sesionId);
            ps.setBigDecimal(2, denominacion);
            ps.setInt(3, cantidad);
            ps.setBigDecimal(4, subtotal);
            ps.executeUpdate();
        }
    }

    public BigDecimal montoEsperadoEfectivo(long sesionId) {
        return Bd.consulta(c -> {
            String sql = "SELECT (SELECT COALESCE(monto_inicial,0) FROM sesion_caja WHERE id = ?) "
                    + "+ COALESCE(SUM(vp.monto - vp.cambio), 0) FROM venta_pago vp "
                    + "JOIN venta v ON v.id = vp.venta_id JOIN medio_pago mp ON mp.id = vp.medio_pago_id "
                    + "WHERE v.sesion_caja_id = ? AND v.estado = 'PAGADA' AND mp.tipo = 'EFECTIVO'";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, sesionId);
                ps.setLong(2, sesionId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getBigDecimal(1);
                }
            }
        });
    }

    public List<SesionCaja> listar(long empresaId, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM sesion_caja s JOIN caja c ON c.id = s.caja_id "
                    + "WHERE s.empresa_id = ? ORDER BY s.id DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setInt(2, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<SesionCaja> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    private SesionCaja mapear(ResultSet rs) throws SQLException {
        return new SesionCaja(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("caja_id"),
                rs.getString("caja"), (Long) rs.getObject("usuario_apertura_id"),
                (Long) rs.getObject("usuario_cierre_id"), rs.getObject("fecha_apertura", LocalDateTime.class),
                rs.getObject("fecha_cierre", LocalDateTime.class), rs.getBigDecimal("monto_inicial"),
                rs.getBigDecimal("monto_esperado_efectivo"), rs.getBigDecimal("monto_contado_efectivo"),
                rs.getBigDecimal("diferencia"), rs.getString("estado"), rs.getString("observacion"));
    }
}
