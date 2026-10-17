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

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.CxcMovimiento;

public final class CxcRepositorio {

    public void registrar(Connection c, long empresaId, long clienteId, Long ventaId, String tipo, BigDecimal monto,
                          BigDecimal saldoResultante, Long medioPagoId, Long usuarioId, String nota) throws SQLException {
        String sql = "INSERT INTO cxc_movimiento (empresa_id, cliente_id, venta_id, tipo, monto, saldo_resultante, "
                + "medio_pago_id, usuario_id, nota) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, clienteId);
            ps.setObject(3, ventaId, Types.BIGINT);
            ps.setString(4, tipo);
            ps.setBigDecimal(5, monto);
            ps.setBigDecimal(6, saldoResultante);
            ps.setObject(7, medioPagoId, Types.BIGINT);
            ps.setObject(8, usuarioId, Types.BIGINT);
            ps.setString(9, nota);
            ps.executeUpdate();
        }
    }

    public List<CxcMovimiento> listar(long empresaId, Long clienteId, int limite) {
        return Bd.consulta(c -> {
            String sql = "SELECT m.id, m.empresa_id, m.cliente_id, cl.nombres, cl.razon_social, m.venta_id, m.tipo, "
                    + "m.monto, m.saldo_resultante, m.medio_pago_id, m.creado_en, m.nota "
                    + "FROM cxc_movimiento m JOIN cliente cl ON cl.id = m.cliente_id "
                    + "WHERE m.empresa_id = ? AND (? IS NULL OR m.cliente_id = ?) ORDER BY m.id DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setObject(2, clienteId, Types.BIGINT);
                ps.setObject(3, clienteId, Types.BIGINT);
                ps.setInt(4, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    List<CxcMovimiento> lista = new ArrayList<>();
                    while (rs.next()) {
                        String nombre = rs.getString("razon_social");
                        if (nombre == null) {
                            nombre = rs.getString("nombres");
                        }
                        lista.add(new CxcMovimiento(rs.getLong("id"), rs.getLong("empresa_id"), rs.getLong("cliente_id"),
                                nombre, (Long) rs.getObject("venta_id"), rs.getString("tipo"), rs.getBigDecimal("monto"),
                                rs.getBigDecimal("saldo_resultante"), (Long) rs.getObject("medio_pago_id"),
                                rs.getObject("creado_en", LocalDateTime.class), rs.getString("nota")));
                    }
                    return lista;
                }
            }
        });
    }
}
