package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Rol;
import ec.sfjfx.core.modelo.Usuario;

public final class UsuarioRepositorio {

    private static final String COLUMNAS_AUTENTICACION =
            "id, empresa_id, nombre_usuario, nombres, apellidos, email, telefono, "
            + "password_hash, pin_hash, es_superadmin, activo, bloqueado, intentos_fallidos";

    // ------------------------------------------------------------------ autenticacion

    public Optional<DatosAutenticacion> porNombreUsuario(String nombreUsuario) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS_AUTENTICACION + " FROM usuario WHERE lower(nombre_usuario) = lower(?)")) {
                ps.setString(1, nombreUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapearAuth(rs)) : Optional.empty();
                }
            }
        });
    }

    public int registrarIntentoFallido(long usuarioId) {
        return Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE usuario SET intentos_fallidos = intentos_fallidos + 1 WHERE id = ? "
                    + "RETURNING intentos_fallidos")) {
                ps.setLong(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    public void bloquear(long usuarioId) {
        Bd.transaccion(c -> {
            ejecutar(c, "UPDATE usuario SET bloqueado = TRUE WHERE id = ?", usuarioId);
            return null;
        });
    }

    public void marcarAcceso(long usuarioId) {
        Bd.transaccion(c -> {
            ejecutar(c, "UPDATE usuario SET intentos_fallidos = 0, bloqueado = FALSE, ultimo_acceso = now() WHERE id = ?",
                    usuarioId);
            return null;
        });
    }

    // ------------------------------------------------------------------ CRUD

    public List<Usuario> listar(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT id, empresa_id, nombre_usuario, nombres, apellidos, email, telefono, "
                    + "es_superadmin, activo FROM usuario "
                    + "WHERE empresa_id = ? OR empresa_id IS NULL ORDER BY nombres, apellidos";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Usuario> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Usuario> porId(long id) {
        return Bd.consulta(c -> {
            String sql = "SELECT id, empresa_id, nombre_usuario, nombres, apellidos, email, telefono, "
                    + "es_superadmin, activo FROM usuario WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public boolean existeNombreUsuario(String nombreUsuario, Long excluirId) {
        return Bd.consulta(c -> {
            String sql = "SELECT 1 FROM usuario WHERE lower(nombre_usuario) = lower(?) "
                    + "AND (? IS NULL OR id <> ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, nombreUsuario);
                if (excluirId == null) {
                    ps.setNull(2, java.sql.Types.BIGINT);
                    ps.setNull(3, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(2, excluirId);
                    ps.setLong(3, excluirId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public long crear(Usuario u, String passwordHash, String pinHash) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO usuario (empresa_id, nombre_usuario, nombres, apellidos, email, "
                    + "telefono, password_hash, pin_hash, es_superadmin, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, u.empresaId());
                ps.setString(2, u.nombreUsuario());
                ps.setString(3, u.nombres());
                ps.setString(4, u.apellidos());
                ps.setString(5, u.email());
                ps.setString(6, u.telefono());
                ps.setString(7, passwordHash);
                ps.setString(8, pinHash);
                ps.setBoolean(9, u.esSuperadmin());
                ps.setBoolean(10, u.activo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Usuario u) {
        Bd.transaccion(c -> {
            String sql = "UPDATE usuario SET nombre_usuario = ?, nombres = ?, apellidos = ?, email = ?, "
                    + "telefono = ?, es_superadmin = ?, activo = ?, actualizado_en = now() WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, u.nombreUsuario());
                ps.setString(2, u.nombres());
                ps.setString(3, u.apellidos());
                ps.setString(4, u.email());
                ps.setString(5, u.telefono());
                ps.setBoolean(6, u.esSuperadmin());
                ps.setBoolean(7, u.activo());
                ps.setLong(8, u.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void cambiarPassword(long id, String hash) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE usuario SET password_hash = ?, intentos_fallidos = 0, bloqueado = FALSE, "
                    + "actualizado_en = now() WHERE id = ?")) {
                ps.setString(1, hash);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void cambiarPin(long id, String hash) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET pin_hash = ? WHERE id = ?")) {
                ps.setString(1, hash);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void cambiarActivo(long id, boolean activo) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET activo = ? WHERE id = ?")) {
                ps.setBoolean(1, activo);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ roles / sucursales

    public List<Rol> rolesDe(long usuarioId) {
        return Bd.consulta(c -> {
            String sql = "SELECT r.id, r.empresa_id, r.nombre, r.descripcion, r.es_sistema, r.activo "
                    + "FROM rol r JOIN usuario_rol ur ON ur.rol_id = r.id "
                    + "WHERE ur.usuario_id = ? ORDER BY r.nombre";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Rol> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new Rol(rs.getLong("id"), (Long) rs.getObject("empresa_id"),
                                rs.getString("nombre"), rs.getString("descripcion"),
                                rs.getBoolean("es_sistema"), rs.getBoolean("activo")));
                    }
                    return lista;
                }
            }
        });
    }

    public void asignarRoles(long usuarioId, Set<Long> rolIds) {
        Bd.transaccion(c -> {
            try (PreparedStatement del = c.prepareStatement("DELETE FROM usuario_rol WHERE usuario_id = ?")) {
                del.setLong(1, usuarioId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO usuario_rol (usuario_id, rol_id) VALUES (?, ?)")) {
                for (Long rolId : rolIds) {
                    ins.setLong(1, usuarioId);
                    ins.setLong(2, rolId);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            return null;
        });
    }

    public Set<Long> sucursalesDe(long usuarioId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT sucursal_id FROM usuario_sucursal WHERE usuario_id = ?")) {
                ps.setLong(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    Set<Long> ids = new java.util.HashSet<>();
                    while (rs.next()) {
                        ids.add(rs.getLong(1));
                    }
                    return ids;
                }
            }
        });
    }

    public void asignarSucursales(long usuarioId, Set<Long> sucursalIds) {
        Bd.transaccion(c -> {
            try (PreparedStatement del = c.prepareStatement("DELETE FROM usuario_sucursal WHERE usuario_id = ?")) {
                del.setLong(1, usuarioId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO usuario_sucursal (usuario_id, sucursal_id) VALUES (?, ?)")) {
                for (Long sucId : sucursalIds) {
                    ins.setLong(1, usuarioId);
                    ins.setLong(2, sucId);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ helpers

    private void ejecutar(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getLong("id"),
                (Long) rs.getObject("empresa_id"),
                rs.getString("nombre_usuario"),
                rs.getString("nombres"),
                rs.getString("apellidos"),
                rs.getString("email"),
                rs.getString("telefono"),
                rs.getBoolean("es_superadmin"),
                rs.getBoolean("activo"));
    }

    private DatosAutenticacion mapearAuth(ResultSet rs) throws SQLException {
        return new DatosAutenticacion(
                rs.getLong("id"),
                (Long) rs.getObject("empresa_id"),
                rs.getString("nombre_usuario"),
                rs.getString("nombres"),
                rs.getString("apellidos"),
                rs.getString("email"),
                rs.getString("telefono"),
                rs.getString("password_hash"),
                rs.getString("pin_hash"),
                rs.getBoolean("es_superadmin"),
                rs.getBoolean("activo"),
                rs.getBoolean("bloqueado"),
                rs.getInt("intentos_fallidos"));
    }
}
