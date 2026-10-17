package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Permiso;

public final class PermisoRepositorio {

    public Set<String> codigosPorUsuario(long usuarioId) {
        return Bd.consulta(c -> {
            String sql = "SELECT DISTINCT p.codigo FROM permiso p "
                    + "JOIN rol_permiso rp ON rp.permiso_id = p.id "
                    + "JOIN usuario_rol ur ON ur.rol_id = rp.rol_id "
                    + "WHERE ur.usuario_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    Set<String> codigos = new HashSet<>();
                    while (rs.next()) {
                        codigos.add(rs.getString(1));
                    }
                    return codigos;
                }
            }
        });
    }

    public Set<String> todosLosCodigos() {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT codigo FROM permiso");
                 ResultSet rs = ps.executeQuery()) {
                Set<String> codigos = new HashSet<>();
                while (rs.next()) {
                    codigos.add(rs.getString(1));
                }
                return codigos;
            }
        });
    }

    public List<Permiso> listarTodos() {
        return Bd.consulta(c -> {
            String sql = "SELECT id, codigo, modulo, accion, descripcion FROM permiso "
                    + "ORDER BY modulo, accion";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<Permiso> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new Permiso(rs.getLong("id"), rs.getString("codigo"),
                            rs.getString("modulo"), rs.getString("accion"), rs.getString("descripcion")));
                }
                return lista;
            }
        });
    }
}
