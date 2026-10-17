package ec.sfjfx.core.repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.modelo.Impuesto;
import ec.sfjfx.core.modelo.TipoImpuesto;

public final class ImpuestoRepositorio {

    private static final String COLUMNAS =
            "id, empresa_id, codigo_sri, nombre, tipo, porcentaje, vigente_desde, vigente_hasta, "
            + "es_retencion, activo";

    public List<Impuesto> listar(long empresaId) {
        return Bd.consulta(c -> {
            String sql = "SELECT " + COLUMNAS + " FROM impuesto WHERE empresa_id = ? OR empresa_id IS NULL "
                    + "ORDER BY tipo, porcentaje DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, empresaId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Impuesto> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            }
        });
    }

    public Optional<Impuesto> porId(long id) {
        return Bd.consulta(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNAS + " FROM impuesto WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public long crear(Impuesto i) {
        return Bd.transaccion(c -> {
            String sql = "INSERT INTO impuesto (empresa_id, codigo_sri, nombre, tipo, porcentaje, "
                    + "vigente_desde, vigente_hasta, es_retencion, activo) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, i);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void actualizar(Impuesto i) {
        Bd.transaccion(c -> {
            String sql = "UPDATE impuesto SET codigo_sri = ?, nombre = ?, tipo = ?, porcentaje = ?, "
                    + "vigente_desde = ?, vigente_hasta = ?, es_retencion = ?, activo = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, i.codigoSri());
                ps.setString(2, i.nombre());
                ps.setString(3, i.tipo().name());
                ps.setBigDecimal(4, i.porcentaje());
                ps.setObject(5, i.vigenteDesde());
                ps.setObject(6, i.vigenteHasta());
                ps.setBoolean(7, i.esRetencion());
                ps.setBoolean(8, i.activo());
                ps.setLong(9, i.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private void asignar(PreparedStatement ps, Impuesto i) throws SQLException {
        ps.setObject(1, i.empresaId());
        ps.setString(2, i.codigoSri());
        ps.setString(3, i.nombre());
        ps.setString(4, i.tipo().name());
        ps.setBigDecimal(5, i.porcentaje());
        ps.setObject(6, i.vigenteDesde() == null ? LocalDate.now() : i.vigenteDesde());
        ps.setObject(7, i.vigenteHasta());
        ps.setBoolean(8, i.esRetencion());
        ps.setBoolean(9, i.activo());
    }

    private Impuesto mapear(ResultSet rs) throws SQLException {
        return new Impuesto(
                rs.getLong("id"),
                (Long) rs.getObject("empresa_id"),
                rs.getString("codigo_sri"),
                rs.getString("nombre"),
                TipoImpuesto.valueOf(rs.getString("tipo")),
                rs.getBigDecimal("porcentaje"),
                rs.getObject("vigente_desde", LocalDate.class),
                rs.getObject("vigente_hasta", LocalDate.class),
                rs.getBoolean("es_retencion"),
                rs.getBoolean("activo"));
    }
}
