package ec.sfjfx.core.repositorio;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Cliente;

public final class ClienteRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, tipo, tipo_identificacion, numero_identificacion, nombres, apellidos, razon_social, "
            + "email, telefono, direccion, es_credito, limite_credito, dias_credito, saldo, activo";

    public List<Cliente> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM cliente WHERE empresa_id = ? AND (? IS NULL OR nombres ILIKE ? "
                    + "OR apellidos ILIKE ? OR razon_social ILIKE ? OR numero_identificacion ILIKE ?) "
                    + "ORDER BY razon_social NULLS LAST, nombres LIMIT 500";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                for (int i = 2; i <= 5; i++) {
                    ps.setString(i, patron);
                }
                ps.setString(6, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Cliente> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Pagina<Cliente> paginar(long empresaId, String filtro, int pagina, int tamano) {
        int p = Math.max(1, pagina);
        int t = Math.min(200, Math.max(1, tamano));
        return Bd.consulta(c -> {
            String patron = (filtro == null || filtro.isBlank()) ? null : "%" + filtro.trim() + "%";
            long total;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM cliente WHERE empresa_id = ? "
                    + "AND (? IS NULL OR nombres ILIKE ? OR apellidos ILIKE ? OR razon_social ILIKE ? "
                    + "OR numero_identificacion ILIKE ?)")) {
                ps.setLong(1, empresaId);
                for (int i = 2; i <= 6; i++) {
                    ps.setString(i, patron);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            List<Cliente> lista = new ArrayList<>();
            String sql = "SELECT " + COLUMNAS + " FROM cliente WHERE empresa_id = ? "
                    + "AND (? IS NULL OR nombres ILIKE ? OR apellidos ILIKE ? OR razon_social ILIKE ? "
                    + "OR numero_identificacion ILIKE ?) "
                    + "ORDER BY razon_social NULLS LAST, nombres OFFSET ? LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                for (int i = 2; i <= 6; i++) {
                    ps.setString(i, patron);
                }
                ps.setInt(7, (p - 1) * t);
                ps.setInt(8, t);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                }
            }
            return new Pagina<>(lista, total, p, t);
        });
    }

    public List<Cliente> conSaldo(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM cliente WHERE empresa_id = ? AND saldo > 0 ORDER BY saldo DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Cliente> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Cliente> porId(long id) {
        return Bd.consulta(c -> porId(c, id));
    }

    public Optional<Cliente> porId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNAS + " FROM cliente WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public boolean identificacionExiste(long empresaId, String numero, Long excluirId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM cliente WHERE empresa_id = ? AND numero_identificacion = ? "
                    + "AND numero_identificacion <> '9999999999' AND (? IS NULL OR id <> ?)")) {
                ps.setLong(1, empresaId);
                ps.setString(2, numero);
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

    public long crear(Cliente cl) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO cliente (empresa_id, tipo, tipo_identificacion, numero_identificacion, nombres, "
                    + "apellidos, razon_social, email, telefono, direccion, es_credito, limite_credito, dias_credito, "
                    + "saldo, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, cl);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Cliente cl) {
        Bd.transaccion(c -> {
            String sql = "UPDATE cliente SET tipo = ?, tipo_identificacion = ?, numero_identificacion = ?, nombres = ?, "
                    + "apellidos = ?, razon_social = ?, email = ?, telefono = ?, direccion = ?, es_credito = ?, "
                    + "limite_credito = ?, dias_credito = ?, activo = ?, actualizado_en = now() WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, cl.tipo());
                ps.setString(2, cl.tipoIdentificacion());
                ps.setString(3, cl.numeroIdentificacion());
                ps.setString(4, cl.nombres());
                ps.setString(5, cl.apellidos());
                ps.setString(6, cl.razonSocial());
                ps.setString(7, cl.email());
                ps.setString(8, cl.telefono());
                ps.setString(9, cl.direccion());
                ps.setBoolean(10, cl.esCredito());
                ps.setBigDecimal(11, nz(cl.limiteCredito()));
                ps.setInt(12, cl.diasCredito());
                ps.setBoolean(13, cl.activo());
                ps.setLong(14, cl.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void actualizarSaldo(Connection c, long clienteId, BigDecimal saldo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE cliente SET saldo = ? WHERE id = ?")) {
            ps.setBigDecimal(1, saldo);
            ps.setLong(2, clienteId);
            ps.executeUpdate();
        }
    }

    private void asignar(PreparedStatement ps, Cliente cl) throws SQLException {
        ps.setLong(1, cl.empresaId());
        ps.setString(2, cl.tipo());
        ps.setString(3, cl.tipoIdentificacion());
        ps.setString(4, cl.numeroIdentificacion());
        ps.setString(5, cl.nombres());
        ps.setString(6, cl.apellidos());
        ps.setString(7, cl.razonSocial());
        ps.setString(8, cl.email());
        ps.setString(9, cl.telefono());
        ps.setString(10, cl.direccion());
        ps.setBoolean(11, cl.esCredito());
        ps.setBigDecimal(12, nz(cl.limiteCredito()));
        ps.setInt(13, cl.diasCredito());
        ps.setBigDecimal(14, nz(cl.saldo()));
        ps.setBoolean(15, cl.activo());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("tipo"),
                rs.getString("tipo_identificacion"), rs.getString("numero_identificacion"), rs.getString("nombres"),
                rs.getString("apellidos"), rs.getString("razon_social"), rs.getString("email"),
                rs.getString("telefono"), rs.getString("direccion"), rs.getBoolean("es_credito"),
                rs.getBigDecimal("limite_credito"), rs.getInt("dias_credito"), rs.getBigDecimal("saldo"),
                rs.getBoolean("activo"));
    }
}
