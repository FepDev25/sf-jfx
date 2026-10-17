package ec.sfjfx.core.repositorio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Transferencia;
import ec.sfjfx.core.modelo.TransferenciaDetalle;

public final class TransferenciaRepositorio {

    public long insertar(Connection c, Transferencia t, List<TransferenciaDetalle> detalles) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO transferencia (empresa_id, numero, sucursal_origen_id, bodega_origen_id, "
                + "sucursal_destino_id, bodega_destino_id, estado, nota) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setLong(1, t.empresaId());
            ps.setString(2, t.numero());
            ps.setLong(3, t.sucursalOrigenId());
            ps.setLong(4, t.bodegaOrigenId());
            ps.setLong(5, t.sucursalDestinoId());
            ps.setLong(6, t.bodegaDestinoId());
            ps.setString(7, t.estado());
            ps.setString(8, t.nota());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getLong(1);
            }
        }
        insertarDetalles(c, id, detalles);
        return id;
    }

    public void insertarDetalles(Connection c, long transferenciaId, List<TransferenciaDetalle> detalles)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO transferencia_detalle (transferencia_id, producto_id, lote_id, cantidad_enviada, "
                + "cantidad_recibida) VALUES (?, ?, ?, ?, 0)")) {
            for (TransferenciaDetalle d : detalles) {
                ps.setLong(1, transferenciaId);
                ps.setLong(2, d.productoId());
                ps.setObject(3, d.loteId(), Types.BIGINT);
                ps.setBigDecimal(4, d.cantidadEnviada());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void marcarEnTransito(Connection c, long id, long usuarioId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE transferencia SET estado = 'EN_TRANSITO', fecha_envio = now(), usuario_envia_id = ? WHERE id = ?")) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void marcarRecibida(Connection c, long id, long usuarioId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE transferencia SET estado = 'RECIBIDA', fecha_recepcion = now(), usuario_recibe_id = ? WHERE id = ?")) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void anular(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE transferencia SET estado = 'ANULADA' WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void registrarCantidadRecibida(Connection c, long detalleId, java.math.BigDecimal cantidad)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE transferencia_detalle SET cantidad_recibida = ? WHERE id = ?")) {
            ps.setBigDecimal(1, cantidad);
            ps.setLong(2, detalleId);
            ps.executeUpdate();
        }
    }

    public Optional<Transferencia> porId(long id) {
        return Bd.consulta(c -> {
            Transferencia cabecera = null;
            String sql = "SELECT t.id, t.empresa_id, t.numero, t.sucursal_origen_id, t.bodega_origen_id, "
                    + "bo.nombre AS bodega_origen, t.sucursal_destino_id, t.bodega_destino_id, bd.nombre AS bodega_destino, "
                    + "t.estado, t.fecha_envio, t.fecha_recepcion, t.nota "
                    + "FROM transferencia t JOIN bodega bo ON bo.id = t.bodega_origen_id "
                    + "JOIN bodega bd ON bd.id = t.bodega_destino_id WHERE t.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cabecera = mapear(rs);
                    }
                }
            }
            return cabecera == null ? Optional.empty() : Optional.of(cabecera);
        });
    }

    public List<TransferenciaDetalle> detalles(long transferenciaId) {
        return Bd.consulta(c -> detallesEn(c, transferenciaId));
    }

    public List<TransferenciaDetalle> detallesEn(Connection c, long transferenciaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT d.id, d.transferencia_id, d.producto_id, p.nombre AS producto, d.lote_id, "
                + "d.cantidad_enviada, d.cantidad_recibida FROM transferencia_detalle d "
                + "JOIN producto p ON p.id = d.producto_id WHERE d.transferencia_id = ? ORDER BY p.nombre")) {
            ps.setLong(1, transferenciaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<TransferenciaDetalle> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new TransferenciaDetalle(rs.getLong("id"), rs.getLong("transferencia_id"),
                            rs.getLong("producto_id"), rs.getString("producto"), (Long) rs.getObject("lote_id"),
                            rs.getBigDecimal("cantidad_enviada"), rs.getBigDecimal("cantidad_recibida")));
                }
                return lista;
            }
        }
    }

    public List<Transferencia> listar(long empresaId, String filtro) {
        return Bd.consulta(c -> {
            String sql = "SELECT t.id, t.empresa_id, t.numero, t.sucursal_origen_id, t.bodega_origen_id, "
                    + "bo.nombre AS bodega_origen, t.sucursal_destino_id, t.bodega_destino_id, bd.nombre AS bodega_destino, "
                    + "t.estado, t.fecha_envio, t.fecha_recepcion, t.nota "
                    + "FROM transferencia t JOIN bodega bo ON bo.id = t.bodega_origen_id "
                    + "JOIN bodega bd ON bd.id = t.bodega_destino_id "
                    + "WHERE t.empresa_id = ? AND (? IS NULL OR t.numero ILIKE ?) ORDER BY t.id DESC LIMIT 200";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String patron = filtro == null || filtro.isBlank() ? null : "%" + filtro + "%";
                ps.setLong(1, empresaId);
                ps.setString(2, patron);
                ps.setString(3, patron);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Transferencia> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    private Transferencia mapear(ResultSet rs) throws SQLException {
        return new Transferencia(
                rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("numero"),
                rs.getLong("sucursal_origen_id"), rs.getLong("bodega_origen_id"), rs.getString("bodega_origen"),
                rs.getLong("sucursal_destino_id"), rs.getLong("bodega_destino_id"), rs.getString("bodega_destino"),
                rs.getString("estado"), rs.getObject("fecha_envio", LocalDateTime.class),
                rs.getObject("fecha_recepcion", LocalDateTime.class), rs.getString("nota"));
    }
}
