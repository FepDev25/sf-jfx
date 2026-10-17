package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Promocion;
import ec.sfjfx.core.modelo.PromocionAlcance;
import ec.sfjfx.core.modelo.PromocionDetalle;

public final class PromocionRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, nombre, descripcion, tipo, valor, cantidad_requerida, cantidad_pagada, prioridad, "
            + "combinable, vigente_desde, vigente_hasta, estado, activo";

    public List<Promocion> listar(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM promocion WHERE empresa_id = ? ORDER BY prioridad DESC, id DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Promocion> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public List<Promocion> activas(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM promocion WHERE empresa_id = ? AND activo = TRUE "
                    + "AND estado = 'ACTIVA' ORDER BY prioridad DESC, id DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Promocion> lista = new ArrayList<>();
                    while (rs.next()) {
                        Promocion p = mapear(rs);
                        lista.add(cargarAlcances(c, p));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Promocion> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNAS + " FROM promocion WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(cargarAlcances(c, mapear(rs)));
                }
            }
        });
    }

    public long crear(Promocion p) {
        return Bd.transaccion(c -> {
            long id = insertarCabecera(c, p);
            guardarHijos(c, id, p);
            return id;
        });
    }

    public void actualizar(Promocion p) {
        Bd.transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE promocion SET nombre = ?, descripcion = ?, tipo = ?, valor = ?, cantidad_requerida = ?, "
                    + "cantidad_pagada = ?, prioridad = ?, combinable = ?, vigente_desde = ?, vigente_hasta = ?, "
                    + "estado = ?, activo = ? WHERE id = ?")) {
                ps.setString(1, p.nombre());
                ps.setString(2, p.descripcion());
                ps.setString(3, p.tipo());
                ps.setBigDecimal(4, p.valor());
                ps.setObject(5, p.cantidadRequerida(), Types.INTEGER);
                ps.setObject(6, p.cantidadPagada(), Types.INTEGER);
                ps.setInt(7, p.prioridad());
                ps.setBoolean(8, p.combinable());
                ps.setObject(9, p.vigenteDesde());
                ps.setObject(10, p.vigenteHasta());
                ps.setString(11, p.estado());
                ps.setBoolean(12, p.activo());
                ps.setLong(13, p.id());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM promocion_alcance WHERE promocion_id = ?")) {
                ps.setLong(1, p.id());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM promocion_detalle WHERE promocion_id = ?")) {
                ps.setLong(1, p.id());
                ps.executeUpdate();
            }
            guardarHijos(c, p.id(), p);
            return null;
        });
    }

    private long insertarCabecera(Connection c, Promocion p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO promocion (empresa_id, nombre, descripcion, tipo, valor, cantidad_requerida, "
                + "cantidad_pagada, prioridad, combinable, vigente_desde, vigente_hasta, estado, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, p.empresaId());
            ps.setString(2, p.nombre());
            ps.setString(3, p.descripcion());
            ps.setString(4, p.tipo());
            ps.setBigDecimal(5, p.valor());
            ps.setObject(6, p.cantidadRequerida(), Types.INTEGER);
            ps.setObject(7, p.cantidadPagada(), Types.INTEGER);
            ps.setInt(8, p.prioridad());
            ps.setBoolean(9, p.combinable());
            ps.setObject(10, p.vigenteDesde());
            ps.setObject(11, p.vigenteHasta());
            ps.setString(12, p.estado());
            ps.setBoolean(13, p.activo());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void guardarHijos(Connection c, long id, Promocion p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO promocion_alcance (promocion_id, tipo_alcance, referencia_id) VALUES (?, ?, ?)")) {
            for (PromocionAlcance a : p.alcances()) {
                ps.setLong(1, id);
                ps.setString(2, a.tipoAlcance());
                ps.setObject(3, a.referenciaId(), Types.BIGINT);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO promocion_detalle (promocion_id, producto_id, cantidad) VALUES (?, ?, ?)")) {
            for (PromocionDetalle d : p.componentes()) {
                ps.setLong(1, id);
                ps.setObject(2, d.productoId(), Types.BIGINT);
                ps.setBigDecimal(3, d.cantidad());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Promocion cargarAlcances(Connection c, Promocion p) throws SQLException {
        List<PromocionAlcance> alcances = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, promocion_id, tipo_alcance, referencia_id FROM promocion_alcance WHERE promocion_id = ?")) {
            ps.setLong(1, p.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alcances.add(new PromocionAlcance(rs.getLong("id"), rs.getLong("promocion_id"),
                            rs.getString("tipo_alcance"), (Long) rs.getObject("referencia_id")));
                }
            }
        }
        List<PromocionDetalle> componentes = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT d.id, d.promocion_id, d.producto_id, pr.nombre AS producto, d.cantidad "
                + "FROM promocion_detalle d LEFT JOIN producto pr ON pr.id = d.producto_id WHERE d.promocion_id = ?")) {
            ps.setLong(1, p.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    componentes.add(new PromocionDetalle(rs.getLong("id"), rs.getLong("promocion_id"),
                            (Long) rs.getObject("producto_id"), rs.getString("producto"), rs.getBigDecimal("cantidad")));
                }
            }
        }
        return new Promocion(p.id(), p.empresaId(), p.nombre(), p.descripcion(), p.tipo(), p.valor(),
                p.cantidadRequerida(), p.cantidadPagada(), p.prioridad(), p.combinable(), p.vigenteDesde(),
                p.vigenteHasta(), p.estado(), p.activo(), alcances, componentes);
    }

    private Promocion mapear(ResultSet rs) throws SQLException {
        return new Promocion(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("nombre"),
                rs.getString("descripcion"), rs.getString("tipo"), rs.getBigDecimal("valor"),
                (Integer) rs.getObject("cantidad_requerida"), (Integer) rs.getObject("cantidad_pagada"),
                rs.getInt("prioridad"), rs.getBoolean("combinable"),
                rs.getObject("vigente_desde", LocalDate.class), rs.getObject("vigente_hasta", LocalDate.class),
                rs.getString("estado"), rs.getBoolean("activo"), List.of(), List.of());
    }
}
