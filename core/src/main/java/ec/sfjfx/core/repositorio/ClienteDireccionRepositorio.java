package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.ClienteDireccion;
import ec.sfjfx.core.modelo.ClienteContacto;

public final class ClienteDireccionRepositorio {

    public List<ClienteDireccion> porCliente(long clienteId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, cliente_id, alias, direccion, referencia, lat, lng, es_principal "
                    + "FROM cliente_direccion WHERE cliente_id = ? ORDER BY es_principal DESC, id")) {
                ps.setLong(1, clienteId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ClienteDireccion> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new ClienteDireccion(rs.getLong("id"), rs.getLong("cliente_id"),
                                rs.getString("alias"), rs.getString("direccion"), rs.getString("referencia"),
                                rs.getBigDecimal("lat"), rs.getBigDecimal("lng"), rs.getBoolean("es_principal")));
                    }
                    return lista;
                }
            }
        });
    }

    public long crear(ClienteDireccion d) {
        return Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO cliente_direccion (cliente_id, alias, direccion, referencia, lat, lng, es_principal) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
                ps.setLong(1, d.clienteId());
                ps.setString(2, d.alias());
                ps.setString(3, d.direccion());
                ps.setString(4, d.referencia());
                ps.setBigDecimal(5, d.lat());
                ps.setBigDecimal(6, d.lng());
                ps.setBoolean(7, d.esPrincipal());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void eliminar(long id) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM cliente_direccion WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public List<ClienteContacto> contactos(long clienteId) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, cliente_id, nombre, telefono, email, cargo FROM cliente_contacto WHERE cliente_id = ?")) {
                ps.setLong(1, clienteId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<ClienteContacto> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new ClienteContacto(rs.getLong("id"), rs.getLong("cliente_id"),
                                rs.getString("nombre"), rs.getString("telefono"), rs.getString("email"),
                                rs.getString("cargo")));
                    }
                    return lista;
                }
            }
        });
    }

    public long crearContacto(ClienteContacto ct) {
        return Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO cliente_contacto (cliente_id, nombre, telefono, email, cargo) "
                    + "VALUES (?, ?, ?, ?, ?) RETURNING id")) {
                ps.setLong(1, ct.clienteId());
                ps.setString(2, ct.nombre());
                ps.setString(3, ct.telefono());
                ps.setString(4, ct.email());
                ps.setString(5, ct.cargo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }
}
