package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Empresa;

public final class EmpresaRepositorio {

    private static final String COLUMNAS =
            "id, ruc, razon_social, nombre_comercial, direccion_matriz, telefono, email, logo_path, "
            + "contribuyente_especial, obligado_llevar_contabilidad, regimen, moneda, activo";

    public Optional<Empresa> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM empresa WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public Optional<Empresa> primeraActiva() {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM empresa WHERE activo = TRUE ORDER BY id LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        });
    }

    public List<Empresa> listarTodas() {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM empresa ORDER BY razon_social");
                 ResultSet rs = ps.executeQuery()) {
                List<Empresa> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
                return lista;
            }
        });
    }

    public long crear(Empresa e) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO empresa (ruc, razon_social, nombre_comercial, direccion_matriz, "
                    + "telefono, email, logo_path, contribuyente_especial, obligado_llevar_contabilidad, "
                    + "regimen, moneda, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, e);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Empresa e) {
        Bd.transaccion(c -> {
            String sql = "UPDATE empresa SET ruc = ?, razon_social = ?, nombre_comercial = ?, "
                    + "direccion_matriz = ?, telefono = ?, email = ?, logo_path = ?, "
                    + "contribuyente_especial = ?, obligado_llevar_contabilidad = ?, regimen = ?, "
                    + "moneda = ?, activo = ?, actualizado_en = now() WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, e);
                ps.setLong(13, e.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void asignar(PreparedStatement ps, Empresa e) throws SQLException {
        ps.setString(1, e.ruc());
        ps.setString(2, e.razonSocial());
        ps.setString(3, e.nombreComercial());
        ps.setString(4, e.direccionMatriz());
        ps.setString(5, e.telefono());
        ps.setString(6, e.email());
        ps.setString(7, e.logoPath());
        ps.setBoolean(8, e.contribuyenteEspecial());
        ps.setBoolean(9, e.obligadoContabilidad());
        ps.setString(10, e.regimen());
        ps.setString(11, e.moneda());
        ps.setBoolean(12, e.activo());
    }

    private Empresa mapear(ResultSet rs) throws SQLException {
        return new Empresa(
                rs.getLong("id"),
                rs.getString("ruc"),
                rs.getString("razon_social"),
                rs.getString("nombre_comercial"),
                rs.getString("direccion_matriz"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("logo_path"),
                rs.getBoolean("contribuyente_especial"),
                rs.getBoolean("obligado_llevar_contabilidad"),
                rs.getString("regimen"),
                rs.getString("moneda"),
                rs.getBoolean("activo"));
    }
}
