package ec.sfjfx.core.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ec.sfjfx.core.db.Bd;
import ec.sfjfx.core.repositorio.HistorialPrecioRepositorio;
import ec.sfjfx.core.seguridad.SesionActual;

/** Ajuste masivo de precios por categoria (porcentaje sobre una lista). */
public final class ServicioPrecioMasivo {

    private final HistorialPrecioRepositorio historial = new HistorialPrecioRepositorio();
    private final ServicioAuditoria auditoria = new ServicioAuditoria();

    public int aplicar(long listaPrecioId, Long categoriaId, BigDecimal porcentaje) {
        long empresa = empresa();
        Long usuario = SesionActual.requerida().usuario().id();
        int afectados = Bd.transaccion(c -> {
            List<Long> productos = productosDeCategoria(c, empresa, categoriaId);
            int n = 0;
            for (Long productoId : productos) {
                BigDecimal actual = precioActual(c, listaPrecioId, productoId);
                if (actual == null || actual.signum() <= 0) {
                    continue;
                }
                BigDecimal nuevo = actual.multiply(BigDecimal.ONE.add(
                        porcentaje.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)))
                        .setScale(4, RoundingMode.HALF_UP);
                actualizarPrecio(c, listaPrecioId, productoId, nuevo);
                historial.registrar(c, empresa, productoId, listaPrecioId, "PRECIO", actual, nuevo, null, usuario);
                n++;
            }
            return n;
        });
        auditoria.registrar("LISTAS_PRECIO", "precio_masivo", null, "EDITAR", null,
                Map.of("lista", listaPrecioId, "categoria", categoriaId == null ? 0 : categoriaId,
                        "porcentaje", porcentaje.toPlainString(), "afectados", afectados));
        return afectados;
    }

    private List<Long> productosDeCategoria(Connection c, long empresa, Long categoriaId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        if (categoriaId == null) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM producto WHERE empresa_id = ? AND activo = TRUE")) {
                ps.setLong(1, empresa);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getLong(1));
                    }
                }
            }
            return ids;
        }
        String sql = "WITH RECURSIVE cat AS ("
                + " SELECT id FROM categoria WHERE empresa_id = ? AND id = ?"
                + " UNION ALL SELECT c.id FROM categoria c JOIN cat ON c.padre_id = cat.id)"
                + " SELECT p.id FROM producto p JOIN cat ON cat.id = p.categoria_id WHERE p.activo = TRUE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, empresa);
            ps.setLong(2, categoriaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    private BigDecimal precioActual(Connection c, long listaId, long productoId) throws SQLException {
        String sql = "SELECT precio FROM precio_producto WHERE lista_precio_id = ? AND producto_id = ? "
                + "AND (vigente_hasta IS NULL OR vigente_hasta >= CURRENT_DATE) "
                + "ORDER BY vigente_desde DESC NULLS LAST LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, listaId);
            ps.setLong(2, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    private void actualizarPrecio(Connection c, long listaId, long productoId, BigDecimal nuevo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE precio_producto SET precio = ? WHERE lista_precio_id = ? AND producto_id = ? "
                + "AND (vigente_hasta IS NULL OR vigente_hasta >= CURRENT_DATE)")) {
            ps.setBigDecimal(1, nuevo);
            ps.setLong(2, listaId);
            ps.setLong(3, productoId);
            ps.executeUpdate();
        }
    }

    private long empresa() {
        return SesionActual.requerida().empresa().id();
    }
}
