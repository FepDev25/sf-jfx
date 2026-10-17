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
import java.util.Map;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.TomaDetalle;
import ec.sfjfx.core.modelo.TomaInventario;

public final class TomaInventarioRepositorio {

    public long insertar(Connection c, TomaInventario t) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO toma_inventario (empresa_id, bodega_id, numero, estado, conteo_ciego, usuario_id, observacion) "
                + "VALUES (?, ?, ?, 'ABIERTA', ?, ?, ?) RETURNING id")) {
            ps.setLong(1, t.empresaId());
            ps.setLong(2, t.bodegaId());
            ps.setString(3, t.numero());
            ps.setBoolean(4, t.conteoCiego());
            ps.setObject(5, usuarioActual(), Types.BIGINT);
            ps.setString(6, t.observacion());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        insertarDetalles(c, id, t.detalles());
        return id;
    }

    public void insertarDetalles(Connection c, long tomaId, List<TomaDetalle> detalles) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO toma_inventario_detalle (toma_id, producto_id, lote_id, cantidad_sistema, "
                + "cantidad_contada, diferencia, costo_unitario) VALUES (?, ?, ?, ?, 0, 0, ?)")) {
            for (TomaDetalle d : detalles) {
                ps.setLong(1, tomaId);
                ps.setLong(2, d.productoId());
                ps.setObject(3, d.loteId(), Types.BIGINT);
                ps.setBigDecimal(4, d.cantidadSistema());
                ps.setBigDecimal(5, d.costoUnitario());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void guardarConteos(Connection c, long tomaId, Map<Long, BigDecimal> conteos) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE toma_inventario_detalle SET cantidad_contada = ?, "
                + "diferencia = ? - cantidad_sistema WHERE toma_id = ? AND producto_id = ? "
                + "AND COALESCE(lote_id, 0) = COALESCE(?, 0)")) {
            for (Map.Entry<Long, BigDecimal> e : conteos.entrySet()) {
                ps.setBigDecimal(1, e.getValue());
                ps.setBigDecimal(2, e.getValue());
                ps.setLong(3, tomaId);
                ps.setLong(4, e.getKey());
                ps.setNull(5, Types.BIGINT);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void cambiarEstado(Connection c, long id, String estado, boolean cerrar) throws SQLException {
        String sql = cerrar
                ? "UPDATE toma_inventario SET estado = ?, fecha_cierre = now() WHERE id = ?"
                : "UPDATE toma_inventario SET estado = ? WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public Optional<TomaInventario> porId(long id) {
        return Bd.consulta(c -> {
            TomaInventario cabecera = null;
            String sql = "SELECT t.id, t.empresa_id, t.bodega_id, b.nombre AS bodega, t.numero, t.estado, "
                    + "t.conteo_ciego, t.fecha_inicio, t.fecha_cierre, t.observacion "
                    + "FROM toma_inventario t JOIN bodega b ON b.id = t.bodega_id WHERE t.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cabecera = new TomaInventario(rs.getLong("id"), rs.getLong("empresa_id"),
                                rs.getLong("bodega_id"), rs.getString("bodega"), rs.getString("numero"),
                                rs.getString("estado"), rs.getBoolean("conteo_ciego"),
                                rs.getObject("fecha_inicio", LocalDateTime.class),
                                rs.getObject("fecha_cierre", LocalDateTime.class), rs.getString("observacion"),
                                new ArrayList<>());
                    }
                }
            }
            if (cabecera == null) {
                return Optional.empty();
            }
            List<TomaDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT d.id, d.toma_id, d.producto_id, p.nombre AS producto, d.lote_id, d.cantidad_sistema, "
                    + "d.cantidad_contada, d.diferencia, d.costo_unitario FROM toma_inventario_detalle d "
                    + "JOIN producto p ON p.id = d.producto_id WHERE d.toma_id = ? ORDER BY p.nombre")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new TomaDetalle(rs.getLong("id"), rs.getLong("toma_id"),
                                rs.getLong("producto_id"), rs.getString("producto"), (Long) rs.getObject("lote_id"),
                                rs.getBigDecimal("cantidad_sistema"), rs.getBigDecimal("cantidad_contada"),
                                rs.getBigDecimal("diferencia"), rs.getBigDecimal("costo_unitario")));
                    }
                }
            }
            return Optional.of(new TomaInventario(cabecera.id(), cabecera.empresaId(), cabecera.bodegaId(),
                    cabecera.bodega(), cabecera.numero(), cabecera.estado(), cabecera.conteoCiego(),
                    cabecera.fechaInicio(), cabecera.fechaCierre(), cabecera.observacion(), detalles));
        });
    }

    public List<TomaInventario> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT t.id, t.empresa_id, t.bodega_id, b.nombre AS bodega, t.numero, t.estado, "
                    + "t.conteo_ciego, t.fecha_inicio, t.fecha_cierre, t.observacion "
                    + "FROM toma_inventario t JOIN bodega b ON b.id = t.bodega_id "
                    + "WHERE t.empresa_id = ? AND (? IS NULL OR t.numero ILIKE ?) ORDER BY t.id DESC LIMIT 200";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<TomaInventario> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(new TomaInventario(rs.getLong("id"), rs.getLong("empresa_id"),
                                rs.getLong("bodega_id"), rs.getString("bodega"), rs.getString("numero"),
                                rs.getString("estado"), rs.getBoolean("conteo_ciego"),
                                rs.getObject("fecha_inicio", LocalDateTime.class),
                                rs.getObject("fecha_cierre", LocalDateTime.class), rs.getString("observacion"),
                                new ArrayList<>()));
                    }
                    return lista;
                }
            }
        });
    }

    private Long usuarioActual() {
        try {
            return ec.sfjfx.core.seguridad.SesionActual.requerida().usuario().id();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
