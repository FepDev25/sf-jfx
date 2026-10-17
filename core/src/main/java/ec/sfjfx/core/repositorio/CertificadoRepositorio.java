package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.CertificadoFirma;

public final class CertificadoRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, alias, ruta_p12, password_cifrada, fecha_desde, fecha_hasta, activo";

    public List<CertificadoFirma> listar(long empresaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM certificado_firma WHERE empresa_id = ? ORDER BY activo DESC, id DESC")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<CertificadoFirma> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<CertificadoFirma> activo(long empresaId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM certificado_firma WHERE empresa_id = ? AND activo = TRUE "
                    + "ORDER BY id DESC LIMIT 1")) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public long crear(CertificadoFirma cf) {
        return Bd.transaccion(c -> {
            desactivarTodos(c, cf.empresaId());
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO certificado_firma (empresa_id, alias, ruta_p12, password_cifrada, fecha_desde, "
                    + "fecha_hasta, activo) VALUES (?, ?, ?, ?, ?, ?, TRUE) RETURNING id")) {
                ps.setLong(1, cf.empresaId());
                ps.setString(2, cf.alias());
                ps.setString(3, cf.rutaP12());
                ps.setString(4, cf.passwordCifrada());
                ps.setObject(5, cf.fechaDesde());
                ps.setObject(6, cf.fechaHasta());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void activar(long empresaId, long id) {
        Bd.transaccion(c -> {
            desactivarTodos(c, empresaId);
            try (PreparedStatement ps = c.prepareStatement("UPDATE certificado_firma SET activo = TRUE WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void eliminar(long id) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM certificado_firma WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void desactivarTodos(java.sql.Connection c, long empresaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE certificado_firma SET activo = FALSE WHERE empresa_id = ?")) {
            ps.setLong(1, empresaId);
            ps.executeUpdate();
        }
    }

    private CertificadoFirma mapear(ResultSet rs) throws SQLException {
        return new CertificadoFirma(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("alias"),
                rs.getString("ruta_p12"), rs.getString("password_cifrada"),
                rs.getObject("fecha_desde", LocalDate.class), rs.getObject("fecha_hasta", LocalDate.class),
                rs.getBoolean("activo"));
    }
}
