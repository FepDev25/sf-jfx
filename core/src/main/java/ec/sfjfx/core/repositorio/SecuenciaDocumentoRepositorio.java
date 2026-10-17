package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ec.sfjfx.core.db.Bd;

public final class SecuenciaDocumentoRepositorio {

    /** Incrementa y devuelve el siguiente secuencial (9 digitos) para la serie indicada. */
    public String siguiente(long empresaId, long sucursalId, String tipo, String establecimiento, String puntoEmision) {
        return Bd.transaccion(c -> {
            asegurar(c, empresaId, sucursalId, tipo, establecimiento, puntoEmision);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE secuencia_documento SET secuencial_actual = secuencial_actual + 1 "
                    + "WHERE empresa_id = ? AND sucursal_id = ? AND tipo_comprobante = ? AND establecimiento = ? "
                    + "AND punto_emision = ? RETURNING secuencial_actual")) {
                ps.setLong(1, empresaId);
                ps.setLong(2, sucursalId);
                ps.setString(3, tipo);
                ps.setString(4, establecimiento);
                ps.setString(5, puntoEmision);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return String.format("%09d", rs.getLong(1));
                }
            }
        });
    }

    private void asegurar(Connection c, long empresaId, long sucursalId, String tipo, String establecimiento,
                          String puntoEmision) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO secuencia_documento (empresa_id, sucursal_id, tipo_comprobante, establecimiento, "
                + "punto_emision, secuencial_actual) VALUES (?, ?, ?, ?, ?, 0) "
                + "ON CONFLICT (empresa_id, sucursal_id, tipo_comprobante, establecimiento, punto_emision) DO NOTHING")) {
            ps.setLong(1, empresaId);
            ps.setLong(2, sucursalId);
            ps.setString(3, tipo);
            ps.setString(4, establecimiento);
            ps.setString(5, puntoEmision);
            ps.executeUpdate();
        }
    }
}
