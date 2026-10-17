package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.ProgramaFidelidad;
import ec.sfjfx.core.modelo.PuntosMovimiento;

public final class FidelidadRepositorio {

    public Optional<ProgramaFidelidad> programa(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT id, empresa_id, nombre, activo, puntos_por_monto, valor_punto, vigencia_puntos_dias "
                    + "FROM programa_fidelidad WHERE empresa_id = ? ORDER BY id LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new ProgramaFidelidad(rs.getLong("id"), rs.getLong("empresa_id"),
                            rs.getString("nombre"), rs.getBoolean("activo"), rs.getBigDecimal("puntos_por_monto"),
                            rs.getBigDecimal("valor_punto"), (Integer) rs.getObject("vigencia_puntos_dias")));
                }
            }
        });
    }

    public void guardarPrograma(ProgramaFidelidad p) {
        Bd.transaccion(c -> {
            if (p.id() == null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO programa_fidelidad (empresa_id, nombre, activo, puntos_por_monto, valor_punto, "
                        + "vigencia_puntos_dias) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setLong(1, p.empresaId());
                    ps.setString(2, p.nombre());
                    ps.setBoolean(3, p.activo());
                    ps.setBigDecimal(4, nz(p.puntosPorMonto()));
                    ps.setBigDecimal(5, nz(p.valorPunto()));
                    ps.setObject(6, p.vigenciaPuntosDias(), Types.INTEGER);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE programa_fidelidad SET nombre = ?, activo = ?, puntos_por_monto = ?, valor_punto = ?, "
                        + "vigencia_puntos_dias = ? WHERE id = ?")) {
                    ps.setString(1, p.nombre());
                    ps.setBoolean(2, p.activo());
                    ps.setBigDecimal(3, nz(p.puntosPorMonto()));
                    ps.setBigDecimal(4, nz(p.valorPunto()));
                    ps.setObject(5, p.vigenciaPuntosDias(), Types.INTEGER);
                    ps.setLong(6, p.id());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    public BigDecimal saldo(Connection c, long clienteId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT saldo_puntos FROM cliente_puntos WHERE cliente_id = ?")) {
            ps.setLong(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
            }
        }
    }

    public void actualizarSaldo(Connection c, long clienteId, BigDecimal saldo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO cliente_puntos (cliente_id, saldo_puntos) VALUES (?, ?) "
                + "ON CONFLICT (cliente_id) DO UPDATE SET saldo_puntos = EXCLUDED.saldo_puntos")) {
            ps.setLong(1, clienteId);
            ps.setBigDecimal(2, saldo);
            ps.executeUpdate();
        }
    }

    public void registrarMovimiento(Connection c, long clienteId, String tipo, BigDecimal puntos, String refTipo,
                                    Long refId, BigDecimal saldoResultante, String nota) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO puntos_movimiento (cliente_id, tipo, puntos, referencia_tipo, referencia_id, "
                + "saldo_resultante, nota) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, clienteId);
            ps.setString(2, tipo);
            ps.setBigDecimal(3, puntos);
            ps.setString(4, refTipo);
            ps.setObject(5, refId, Types.BIGINT);
            ps.setBigDecimal(6, saldoResultante);
            ps.setString(7, nota);
            ps.executeUpdate();
        }
    }

    public List<PuntosMovimiento> movimientos(long clienteId, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT m.id, m.cliente_id, cl.nombres, cl.razon_social, m.tipo, m.puntos, "
                    + "m.referencia_tipo, m.referencia_id, m.saldo_resultante, m.fecha, m.nota "
                    + "FROM puntos_movimiento m JOIN cliente cl ON cl.id = m.cliente_id "
                    + "WHERE m.cliente_id = ? ORDER BY m.id DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, clienteId);
                ps.setInt(2, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<PuntosMovimiento> lista = new ArrayList<>();
                    while (rs.next()) {
                        String nombre = rs.getString("razon_social");
                        if (nombre == null) {
                            nombre = rs.getString("nombres");
                        }
                        lista.add(new PuntosMovimiento(rs.getLong("id"), rs.getLong("cliente_id"), nombre,
                                rs.getString("tipo"), rs.getBigDecimal("puntos"), rs.getString("referencia_tipo"),
                                (Long) rs.getObject("referencia_id"), rs.getBigDecimal("saldo_resultante"),
                                rs.getObject("fecha", LocalDateTime.class), rs.getString("nota")));
                    }
                    return lista;
                }
            }
        });
    }

    /** Clientes con saldo de puntos (para el tablero). */
    public List<Object[]> clientesConPuntos(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT cl.id, cl.nombres, cl.razon_social, cp.saldo_puntos FROM cliente_puntos cp "
                    + "JOIN cliente cl ON cl.id = cp.cliente_id WHERE cl.empresa_id = ? AND cp.saldo_puntos <> 0 "
                    + "ORDER BY cp.saldo_puntos DESC LIMIT 500";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Object[]> lista = new ArrayList<>();
                    while (rs.next()) {
                        String nombre = rs.getString("razon_social");
                        if (nombre == null) {
                            nombre = rs.getString("nombres");
                        }
                        lista.add(new Object[]{rs.getLong("id"), nombre, rs.getBigDecimal("saldo_puntos")});
                    }
                    return lista;
                }
            }
        });
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
