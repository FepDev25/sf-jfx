package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Configuracion;

public final class ConfiguracionRepositorio {

    private static final String COLUMNAS = "id, empresa_id, clave, valor, tipo, grupo, descripcion";

    public List<Configuracion> porEmpresa(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM configuracion WHERE empresa_id = ? ORDER BY grupo, clave";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Configuracion> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Configuracion> porClave(long empresaId, String clave) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM configuracion WHERE empresa_id = ? AND clave = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setString(2, clave);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public String valor(long empresaId, String clave, String porDefecto) {
        return porClave(empresaId, clave).map(Configuracion::valor).orElse(porDefecto);
    }

    public void guardar(long empresaId, String clave, String valor, String tipo, String grupo, String descripcion) {
        Bd.transaccion(c -> {
            String sql = "INSERT INTO configuracion (empresa_id, clave, valor, tipo, grupo, descripcion) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (empresa_id, clave) DO UPDATE SET valor = EXCLUDED.valor, "
                    + "tipo = EXCLUDED.tipo, grupo = EXCLUDED.grupo, descripcion = EXCLUDED.descripcion";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                ps.setString(2, clave);
                ps.setString(3, valor);
                ps.setString(4, tipo);
                ps.setString(5, grupo);
                ps.setString(6, descripcion);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void eliminar(long id) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM configuracion WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Configuracion mapear(ResultSet rs) throws SQLException {
        return new Configuracion(
                rs.getLong("id"),
                rs.getLong("empresa_id"),
                rs.getString("clave"),
                rs.getString("valor"),
                rs.getString("tipo"),
                rs.getString("grupo"),
                rs.getString("descripcion"));
    }
}
