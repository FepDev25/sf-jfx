package ec.sfjfx.core.reporte;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ec.sfjfx.core.db.Bd;

/** Consultas SQL de los reportes y del panel de inicio. */
public final class RepositorioReportes {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    public ResultadoReporte ejecutar(DefinicionReporte definicion, long empresaId, FiltroReporte filtro,
                                     int umbralStockBajo) {
        return switch (definicion.codigo()) {
            case "ventas_por_dia" -> ventasPorDia(definicion, empresaId, filtro);
            case "ventas_por_cajero" -> ventasPorCajero(definicion, empresaId, filtro);
            case "ventas_por_producto" -> ventasPorProducto(definicion, empresaId, filtro);
            case "ventas_por_categoria" -> ventasPorCategoria(definicion, empresaId, filtro);
            case "ventas_por_medio_pago" -> ventasPorMedioPago(definicion, empresaId, filtro);
            case "stock_valorizado" -> stockValorizado(definicion, empresaId, filtro);
            case "stock_bajo" -> stockBajo(definicion, empresaId, filtro, umbralStockBajo);
            case "lotes_por_vencer" -> lotesPorVencer(definicion, empresaId, filtro);
            case "kardex_por_tipo" -> kardexPorTipo(definicion, empresaId, filtro);
            case "compras_por_proveedor" -> comprasPorProveedor(definicion, empresaId, filtro);
            case "cuentas_por_pagar" -> cuentasPorPagar(definicion, empresaId, filtro);
            case "pagos_a_proveedores" -> pagosAProveedores(definicion, empresaId, filtro);
            case "cuentas_por_cobrar" -> cuentasPorCobrar(definicion, empresaId, filtro);
            case "utilidad_por_dia" -> utilidadPorDia(definicion, empresaId, filtro);
            case "abonos_de_clientes" -> abonosDeClientes(definicion, empresaId, filtro);
            case "iva_por_dia" -> ivaPorDia(definicion, empresaId, filtro);
            case "comprobantes_por_tipo" -> comprobantesPorTipo(definicion, empresaId, filtro);
            case "impuestos_por_tarifa" -> impuestosPorTarifa(definicion, empresaId, filtro);
            case "sesiones_de_caja" -> sesionesDeCaja(definicion, empresaId, filtro);
            case "ventas_por_caja" -> ventasPorCaja(definicion, empresaId, filtro);
            case "devoluciones_de_venta" -> devolucionesDeVenta(definicion, empresaId, filtro);
            case "anulaciones_de_venta" -> anulacionesDeVenta(definicion, empresaId, filtro);
            default -> throw new IllegalArgumentException("Reporte desconocido: " + definicion.codigo());
        };
    }

    // ------------------------------------------------------------------ panel

    public PanelDashboard dashboard(long empresaId, Long sucursalId, int umbralStockBajo) {
        return Bd.consulta(c -> {
            BigDecimal ventas = BigDecimal.ZERO;
            BigDecimal ticket = BigDecimal.ZERO;
            BigDecimal utilidad = BigDecimal.ZERO;
            long cantidad = 0;

            StringBuilder sql = new StringBuilder(
                    "SELECT COALESCE(SUM(v.total), 0) total, COALESCE(AVG(v.total), 0) ticket, "
                    + "COALESCE(SUM(v.utilidad), 0) utilidad, COUNT(*) cantidad "
                    + "FROM venta v WHERE v.empresa_id = ? AND v.estado = 'PAGADA' "
                    + "AND v.fecha >= CURRENT_DATE AND v.fecha < CURRENT_DATE + INTERVAL '1 day'");
            List<Object> params = new ArrayList<>();
            params.add(empresaId);
            if (sucursalId != null) {
                sql.append(" AND v.sucursal_id = ?");
                params.add(sucursalId);
            }
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                asignar(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ventas = rs.getBigDecimal("total");
                        ticket = rs.getBigDecimal("ticket");
                        utilidad = rs.getBigDecimal("utilidad");
                        cantidad = rs.getLong("cantidad");
                    }
                }
            }

            BigDecimal cxc = escalar(c, "SELECT COALESCE(SUM(saldo), 0) FROM cliente WHERE empresa_id = ? AND saldo > 0",
                    List.of(empresaId));
            BigDecimal cxp = escalar(c, "SELECT COALESCE(SUM(saldo), 0) FROM factura_proveedor "
                    + "WHERE empresa_id = ? AND estado_pago IN ('PENDIENTE', 'PARCIAL') AND saldo > 0",
                    List.of(empresaId));
            long bajoStock = consultarBajoStock(c, empresaId, sucursalId, umbralStockBajo).size();
            long sesiones = contar(c, "SELECT COUNT(*) FROM sesion_caja WHERE empresa_id = ? AND estado = 'ABIERTA'",
                    List.of(empresaId));

            List<SerieReporte> ultimos = ventasUltimosDias(c, empresaId, sucursalId, 7);
            List<SerieReporte> medios = mediosDePago(c, empresaId, sucursalId);
            List<SerieReporte> top = topProductos(c, empresaId, sucursalId);

            return new PanelDashboard(LocalDate.now(), ventas, ticket, utilidad, cxc, cxp, cantidad,
                    bajoStock, sesiones, ultimos, medios, top);
        });
    }

    private List<SerieReporte> ventasUltimosDias(Connection c, long empresaId, Long sucursalId, int dias)
            throws SQLException {
        LocalDate desde = LocalDate.now().minusDays(dias - 1L);
        StringBuilder sql = new StringBuilder("SELECT v.fecha::date dia, COALESCE(SUM(v.total), 0) total "
                + "FROM venta v WHERE v.empresa_id = ? AND v.estado = 'PAGADA' AND v.fecha::date >= ?");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);
        params.add(desde);
        if (sucursalId != null) {
            sql.append(" AND v.sucursal_id = ?");
            params.add(sucursalId);
        }
        sql.append(" GROUP BY 1 ORDER BY 1");
        List<SerieReporte> serie = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate dia = rs.getObject("dia", LocalDate.class);
                    serie.add(new SerieReporte(dia.format(DIA), rs.getBigDecimal("total")));
                }
            }
        }
        return serie;
    }

    private List<SerieReporte> mediosDePago(Connection c, long empresaId, Long sucursalId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT mp.nombre, COALESCE(SUM(GREATEST(vp.monto - vp.cambio, 0)), 0) total "
                + "FROM venta v JOIN venta_pago vp ON vp.venta_id = v.id "
                + "JOIN medio_pago mp ON mp.id = vp.medio_pago_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'PAGADA' AND v.fecha >= CURRENT_DATE - INTERVAL '30 days'");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);
        if (sucursalId != null) {
            sql.append(" AND v.sucursal_id = ?");
            params.add(sucursalId);
        }
        sql.append(" GROUP BY mp.id, mp.nombre ORDER BY total DESC");
        return serie(c, sql.toString(), params, "nombre", "total", false);
    }

    private List<SerieReporte> topProductos(Connection c, long empresaId, Long sucursalId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(p.nombre, vd.descripcion) producto, COALESCE(SUM(vd.total), 0) total "
                + "FROM venta v JOIN venta_detalle vd ON vd.venta_id = v.id "
                + "LEFT JOIN producto p ON p.id = vd.producto_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'PAGADA' AND v.fecha >= CURRENT_DATE - INTERVAL '30 days'");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);
        if (sucursalId != null) {
            sql.append(" AND v.sucursal_id = ?");
            params.add(sucursalId);
        }
        sql.append(" GROUP BY p.id, p.nombre, vd.descripcion ORDER BY total DESC LIMIT 6");
        return serie(c, sql.toString(), params, "producto", "total", false);
    }

    // ------------------------------------------------------------------ ventas

    private ResultadoReporte ventasPorDia(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.fecha("fecha", "Fecha"),
                ColumnaReporte.entero("ventas", "Ventas"),
                ColumnaReporte.moneda("subtotal", "Subtotal"),
                ColumnaReporte.moneda("descuento", "Descuento"),
                ColumnaReporte.moneda("iva", "IVA"),
                ColumnaReporte.moneda("total", "Total"),
                ColumnaReporte.moneda("utilidad", "Utilidad"));
        StringBuilder sql = new StringBuilder(
                "SELECT v.fecha::date fecha, COUNT(*) ventas, COALESCE(SUM(v.subtotal), 0) subtotal, "
                + "COALESCE(SUM(v.descuento_total), 0) descuento, COALESCE(SUM(v.impuesto_total), 0) iva, "
                + "COALESCE(SUM(v.total), 0) total, COALESCE(SUM(v.utilidad), 0) utilidad "
                + "FROM venta v WHERE v.empresa_id = ? AND v.estado = 'PAGADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, false);
        sql.append(" GROUP BY 1 ORDER BY 1");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 5));
    }

    private ResultadoReporte ventasPorCajero(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("cajero", "Cajero"),
                ColumnaReporte.entero("ventas", "Ventas"),
                ColumnaReporte.moneda("total", "Total"),
                ColumnaReporte.moneda("ticket", "Ticket promedio"));
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(NULLIF(TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellidos, '')), ''), "
                + "'Sin usuario') cajero, COUNT(*) ventas, COALESCE(SUM(v.total), 0) total, "
                + "COALESCE(AVG(v.total), 0) ticket "
                + "FROM venta v LEFT JOIN usuario u ON u.id = v.usuario_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'PAGADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, false);
        sql.append(" GROUP BY 1 ORDER BY total DESC");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 2));
    }

    private ResultadoReporte ventasPorProducto(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("codigo", "Codigo"),
                ColumnaReporte.texto("producto", "Producto"),
                ColumnaReporte.decimal("cantidad", "Cantidad"),
                ColumnaReporte.moneda("total", "Total"),
                ColumnaReporte.moneda("utilidad", "Utilidad"));
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(p.codigo, '') codigo, COALESCE(p.nombre, vd.descripcion) producto, "
                + "COALESCE(SUM(vd.cantidad), 0) cantidad, COALESCE(SUM(vd.total), 0) total, "
                + "COALESCE(SUM(vd.total - vd.costo_unitario * vd.cantidad), 0) utilidad "
                + "FROM venta v JOIN venta_detalle vd ON vd.venta_id = v.id "
                + "LEFT JOIN producto p ON p.id = vd.producto_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'PAGADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, true);
        sql.append(" GROUP BY p.id, p.codigo, p.nombre, vd.descripcion ORDER BY total DESC LIMIT 100");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 1, 3));
    }

    private ResultadoReporte ventasPorCategoria(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("categoria", "Categoria"),
                ColumnaReporte.decimal("cantidad", "Cantidad"),
                ColumnaReporte.moneda("total", "Total"));
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(cat.nombre, 'Sin categoria') categoria, "
                + "COALESCE(SUM(vd.cantidad), 0) cantidad, COALESCE(SUM(vd.total), 0) total "
                + "FROM venta v JOIN venta_detalle vd ON vd.venta_id = v.id "
                + "LEFT JOIN producto p ON p.id = vd.producto_id "
                + "LEFT JOIN categoria cat ON cat.id = p.categoria_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'PAGADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, true);
        sql.append(" GROUP BY cat.id, cat.nombre ORDER BY total DESC");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 2));
    }

    private ResultadoReporte ventasPorMedioPago(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("medio", "Medio de pago"),
                ColumnaReporte.entero("pagos", "Pagos"),
                ColumnaReporte.moneda("total", "Total"));
        StringBuilder sql = new StringBuilder(
                "SELECT mp.nombre medio, COUNT(*) pagos, "
                + "COALESCE(SUM(GREATEST(vp.monto - vp.cambio, 0)), 0) total "
                + "FROM venta v JOIN venta_pago vp ON vp.venta_id = v.id "
                + "JOIN medio_pago mp ON mp.id = vp.medio_pago_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'PAGADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, false);
        sql.append(" GROUP BY mp.id, mp.nombre ORDER BY total DESC");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 2));
    }

    // ------------------------------------------------------------------ inventario

    private ResultadoReporte stockValorizado(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("codigo", "Codigo"),
                ColumnaReporte.texto("producto", "Producto"),
                ColumnaReporte.texto("bodega", "Bodega"),
                ColumnaReporte.decimal("cantidad", "Cantidad"),
                ColumnaReporte.moneda("costo", "Costo promedio"),
                ColumnaReporte.moneda("valor", "Valor"));
        StringBuilder sql = new StringBuilder(
                "SELECT p.codigo, p.nombre producto, b.nombre bodega, "
                + "COALESCE(SUM(s.cantidad - s.cantidad_reservada), 0) cantidad, "
                + "p.costo_promedio costo, "
                + "COALESCE(SUM((s.cantidad - s.cantidad_reservada) * p.costo_promedio), 0) valor "
                + "FROM stock s JOIN producto p ON p.id = s.producto_id "
                + "JOIN bodega b ON b.id = s.bodega_id "
                + "WHERE p.empresa_id = ?");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroInventario(sql, p, "b", "p", f);
        sql.append(" GROUP BY p.id, p.codigo, p.nombre, b.id, b.nombre, p.costo_promedio "
                + "HAVING SUM(s.cantidad - s.cantidad_reservada) <> 0 ORDER BY valor DESC LIMIT 200");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 1, 5));
    }

    private ResultadoReporte stockBajo(DefinicionReporte def, long empresaId, FiltroReporte f, int umbral) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("codigo", "Codigo"),
                ColumnaReporte.texto("producto", "Producto"),
                ColumnaReporte.decimal("disponible", "Disponible"),
                ColumnaReporte.moneda("costo", "Costo promedio"),
                ColumnaReporte.moneda("valor", "Valor"));
        StringBuilder sql = new StringBuilder(
                "SELECT p.codigo, p.nombre producto, "
                + "COALESCE(SUM(s.cantidad - s.cantidad_reservada), 0) disponible, "
                + "p.costo_promedio costo, "
                + "COALESCE(SUM(s.cantidad - s.cantidad_reservada) * p.costo_promedio, 0) valor "
                + "FROM producto p LEFT JOIN stock s ON s.producto_id = p.id "
                + "LEFT JOIN bodega b ON b.id = s.bodega_id "
                + "WHERE p.empresa_id = ? AND p.activo = TRUE AND p.tipo <> 'SERVICIO' "
                + "AND (CAST(? AS BIGINT) IS NULL OR b.sucursal_id = ?)");
        List<Object> p = new ArrayList<>();
        p.add(empresaId);
        p.add(f.sucursalId());
        p.add(f.sucursalId());
        if (f.categoriaId() != null) {
            sql.append(" AND p.categoria_id = ?");
            p.add(f.categoriaId());
        }
        sql.append(" GROUP BY p.id, p.codigo, p.nombre, p.costo_promedio "
                + "HAVING COALESCE(SUM(s.cantidad - s.cantidad_reservada), 0) <= ? AND COUNT(s.id) > 0 "
                + "ORDER BY disponible ASC, p.nombre LIMIT 200");
        p.add(umbral);
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 1, 2));
    }

    private ResultadoReporte lotesPorVencer(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("codigo", "Codigo"),
                ColumnaReporte.texto("producto", "Producto"),
                ColumnaReporte.texto("lote", "Lote"),
                ColumnaReporte.fecha("vence", "Vence"),
                ColumnaReporte.decimal("cantidad", "Cantidad"),
                ColumnaReporte.entero("dias", "Dias"));
        StringBuilder sql = new StringBuilder(
                "SELECT p.codigo, p.nombre producto, l.codigo lote, l.fecha_vencimiento vence, "
                + "COALESCE(SUM(s.cantidad - s.cantidad_reservada), 0) cantidad, "
                + "(l.fecha_vencimiento - CURRENT_DATE) dias "
                + "FROM lote l JOIN producto p ON p.id = l.producto_id "
                + "LEFT JOIN stock s ON s.lote_id = l.id "
                + "LEFT JOIN bodega b ON b.id = s.bodega_id "
                + "WHERE p.empresa_id = ? AND l.fecha_vencimiento IS NOT NULL "
                + "AND l.fecha_vencimiento <= CURRENT_DATE + INTERVAL '90 days' "
                + "AND (CAST(? AS BIGINT) IS NULL OR b.sucursal_id = ?)");
        List<Object> p = new ArrayList<>();
        p.add(empresaId);
        p.add(f.sucursalId());
        p.add(f.sucursalId());
        if (f.categoriaId() != null) {
            sql.append(" AND p.categoria_id = ?");
            p.add(f.categoriaId());
        }
        sql.append(" GROUP BY p.id, p.codigo, p.nombre, l.id, l.codigo, l.fecha_vencimiento "
                + "HAVING COALESCE(SUM(s.cantidad - s.cantidad_reservada), 0) > 0 "
                + "ORDER BY l.fecha_vencimiento LIMIT 200");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, List.of());
    }

    private ResultadoReporte kardexPorTipo(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("tipo", "Tipo de movimiento"),
                ColumnaReporte.entero("movimientos", "Movimientos"),
                ColumnaReporte.decimal("cantidad", "Cantidad neta"),
                ColumnaReporte.moneda("valor", "Valor"));
        StringBuilder sql = new StringBuilder(
                "SELECT tm.nombre tipo, COUNT(*) movimientos, COALESCE(SUM(mi.cantidad), 0) cantidad, "
                + "COALESCE(SUM(ABS(mi.cantidad) * mi.costo_unitario), 0) valor "
                + "FROM movimiento_inventario mi "
                + "JOIN tipo_movimiento_inventario tm ON tm.id = mi.tipo_movimiento_id "
                + "JOIN producto p ON p.id = mi.producto_id "
                + "WHERE mi.empresa_id = ?");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        if (f.sucursalId() != null) {
            sql.append(" AND mi.sucursal_id = ?");
            p.add(f.sucursalId());
        }
        if (f.categoriaId() != null) {
            sql.append(" AND p.categoria_id = ?");
            p.add(f.categoriaId());
        }
        if (f.usuarioId() != null) {
            sql.append(" AND mi.usuario_id = ?");
            p.add(f.usuarioId());
        }
        filtroFecha(sql, p, "mi.creado_en::date", f);
        sql.append(" GROUP BY tm.id, tm.nombre ORDER BY valor DESC");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 3));
    }

    // ------------------------------------------------------------------ compras

    private ResultadoReporte comprasPorProveedor(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("proveedor", "Proveedor"),
                ColumnaReporte.entero("facturas", "Facturas"),
                ColumnaReporte.moneda("subtotal", "Subtotal"),
                ColumnaReporte.moneda("iva", "IVA"),
                ColumnaReporte.moneda("total", "Total"),
                ColumnaReporte.moneda("saldo", "Saldo"));
        StringBuilder sql = new StringBuilder(
                "SELECT pr.razon_social proveedor, COUNT(f.id) facturas, "
                + "COALESCE(SUM(f.subtotal), 0) subtotal, COALESCE(SUM(f.iva), 0) iva, "
                + "COALESCE(SUM(f.total), 0) total, COALESCE(SUM(f.saldo), 0) saldo "
                + "FROM factura_proveedor f JOIN proveedor pr ON pr.id = f.proveedor_id "
                + "LEFT JOIN orden_compra oc ON oc.id = f.orden_compra_id "
                + "WHERE f.empresa_id = ? AND f.estado_pago <> 'ANULADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroFecha(sql, p, "f.fecha_emision", f);
        if (f.sucursalId() != null) {
            sql.append(" AND (oc.sucursal_id = ? OR f.orden_compra_id IS NULL)");
            p.add(f.sucursalId());
        }
        sql.append(" GROUP BY pr.id, pr.razon_social ORDER BY total DESC");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 4));
    }

    private ResultadoReporte cuentasPorPagar(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("proveedor", "Proveedor"),
                ColumnaReporte.texto("factura", "Factura"),
                ColumnaReporte.fecha("emision", "Emision"),
                ColumnaReporte.fecha("vencimiento", "Vencimiento"),
                ColumnaReporte.moneda("total", "Total"),
                ColumnaReporte.moneda("saldo", "Saldo"),
                ColumnaReporte.texto("estado", "Estado"),
                ColumnaReporte.texto("situacion", "Situacion"));
        StringBuilder sql = new StringBuilder(
                "SELECT pr.razon_social proveedor, f.numero factura, f.fecha_emision emision, "
                + "f.fecha_vencimiento vencimiento, f.total, f.saldo, f.estado_pago estado, "
                + "CASE WHEN f.fecha_vencimiento IS NOT NULL AND f.fecha_vencimiento < CURRENT_DATE "
                + "THEN 'VENCIDA' ELSE 'VIGENTE' END situacion "
                + "FROM factura_proveedor f JOIN proveedor pr ON pr.id = f.proveedor_id "
                + "LEFT JOIN orden_compra oc ON oc.id = f.orden_compra_id "
                + "WHERE f.empresa_id = ? AND f.estado_pago IN ('PENDIENTE', 'PARCIAL') AND f.saldo > 0");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroFecha(sql, p, "f.fecha_emision", f);
        if (f.sucursalId() != null) {
            sql.append(" AND (oc.sucursal_id = ? OR f.orden_compra_id IS NULL)");
            p.add(f.sucursalId());
        }
        sql.append(" ORDER BY f.fecha_vencimiento NULLS LAST, f.fecha_emision");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, List.of());
    }

    private ResultadoReporte pagosAProveedores(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.fecha("fecha", "Fecha"),
                ColumnaReporte.texto("proveedor", "Proveedor"),
                ColumnaReporte.texto("numero", "Numero"),
                ColumnaReporte.texto("medio", "Medio"),
                ColumnaReporte.moneda("total", "Total"),
                ColumnaReporte.texto("referencia", "Referencia"));
        StringBuilder sql = new StringBuilder(
                "SELECT pg.fecha::date fecha, pr.razon_social proveedor, pg.numero, "
                + "COALESCE(mp.nombre, '') medio, pg.total, COALESCE(pg.referencia, '') referencia "
                + "FROM pago_proveedor pg JOIN proveedor pr ON pr.id = pg.proveedor_id "
                + "LEFT JOIN medio_pago mp ON mp.id = pg.medio_pago_id "
                + "WHERE pg.empresa_id = ?");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroFecha(sql, p, "pg.fecha::date", f);
        if (f.usuarioId() != null) {
            sql.append(" AND pg.usuario_id = ?");
            p.add(f.usuarioId());
        }
        sql.append(" ORDER BY pg.fecha DESC LIMIT 300");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, List.of());
    }

    // ------------------------------------------------------------------ financiero

    private ResultadoReporte cuentasPorCobrar(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("cliente", "Cliente"),
                ColumnaReporte.entero("movimientos", "Movimientos"),
                ColumnaReporte.moneda("cargos", "Cargos"),
                ColumnaReporte.moneda("abonos", "Abonos"),
                ColumnaReporte.moneda("saldo", "Saldo"));
        StringBuilder sql = new StringBuilder(
                "SELECT CASE WHEN c.razon_social IS NOT NULL AND c.razon_social <> '' THEN c.razon_social "
                + "ELSE TRIM(COALESCE(c.nombres, '') || ' ' || COALESCE(c.apellidos, '')) END cliente, "
                + "COUNT(m.id) movimientos, "
                + "COALESCE(SUM(CASE WHEN m.tipo = 'CARGO' THEN m.monto ELSE 0 END), 0) cargos, "
                + "COALESCE(SUM(CASE WHEN m.tipo = 'ABONO' THEN m.monto ELSE 0 END), 0) abonos, "
                + "c.saldo "
                + "FROM cliente c LEFT JOIN cxc_movimiento m ON m.cliente_id = c.id");
        List<Object> p = new ArrayList<>();
        if (f.desde() != null || f.hasta() != null) {
            sql.append(" AND ");
            List<String> condiciones = new ArrayList<>();
            if (f.desde() != null) {
                condiciones.add("m.creado_en::date >= ?");
                p.add(f.desde());
            }
            if (f.hasta() != null) {
                condiciones.add("m.creado_en::date <= ?");
                p.add(f.hasta());
            }
            sql.append(String.join(" AND ", condiciones));
        }
        sql.append(" WHERE c.empresa_id = ? AND c.saldo <> 0");
        p.add(empresaId);
        if (f.usuarioId() != null) {
            sql.append(" AND m.usuario_id = ?");
            p.add(f.usuarioId());
        }
        sql.append(" GROUP BY c.id, c.razon_social, c.nombres, c.apellidos, c.saldo ORDER BY c.saldo DESC");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 4));
    }

    private ResultadoReporte utilidadPorDia(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.fecha("fecha", "Fecha"),
                ColumnaReporte.moneda("ventas", "Ventas"),
                ColumnaReporte.moneda("costo", "Costo"),
                ColumnaReporte.moneda("utilidad", "Utilidad"),
                ColumnaReporte.porcentaje("margen", "Margen"));
        StringBuilder sql = new StringBuilder(
                "SELECT v.fecha::date fecha, COALESCE(SUM(v.total), 0) ventas, "
                + "COALESCE(SUM(v.costo_total), 0) costo, COALESCE(SUM(v.utilidad), 0) utilidad, "
                + "CASE WHEN SUM(v.total) <> 0 THEN SUM(v.utilidad) / SUM(v.total) * 100 ELSE 0 END margen "
                + "FROM venta v WHERE v.empresa_id = ? AND v.estado = 'PAGADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, false);
        sql.append(" GROUP BY 1 ORDER BY 1");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 3));
    }

    private ResultadoReporte abonosDeClientes(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.fecha("fecha", "Fecha"),
                ColumnaReporte.texto("cliente", "Cliente"),
                ColumnaReporte.moneda("monto", "Monto"),
                ColumnaReporte.texto("medio", "Medio"),
                ColumnaReporte.texto("nota", "Nota"));
        StringBuilder sql = new StringBuilder(
                "SELECT m.creado_en::date fecha, "
                + "CASE WHEN c.razon_social IS NOT NULL AND c.razon_social <> '' THEN c.razon_social "
                + "ELSE TRIM(COALESCE(c.nombres, '') || ' ' || COALESCE(c.apellidos, '')) END cliente, "
                + "m.monto, COALESCE(mp.nombre, '') medio, COALESCE(m.nota, '') nota "
                + "FROM cxc_movimiento m JOIN cliente c ON c.id = m.cliente_id "
                + "LEFT JOIN medio_pago mp ON mp.id = m.medio_pago_id "
                + "WHERE m.empresa_id = ? AND m.tipo = 'ABONO'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroFecha(sql, p, "m.creado_en::date", f);
        if (f.usuarioId() != null) {
            sql.append(" AND m.usuario_id = ?");
            p.add(f.usuarioId());
        }
        sql.append(" ORDER BY m.creado_en DESC LIMIT 300");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, List.of());
    }

    // ------------------------------------------------------------------ impuestos

    private ResultadoReporte ivaPorDia(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.fecha("fecha", "Fecha"),
                ColumnaReporte.moneda("base", "Base imponible"),
                ColumnaReporte.moneda("iva", "IVA"),
                ColumnaReporte.moneda("total", "Total"));
        StringBuilder sql = new StringBuilder(
                "SELECT v.fecha::date fecha, COALESCE(SUM(vd.subtotal), 0) base, "
                + "COALESCE(SUM(vd.impuesto_valor), 0) iva, COALESCE(SUM(vd.total), 0) total "
                + "FROM venta v JOIN venta_detalle vd ON vd.venta_id = v.id "
                + "LEFT JOIN producto p ON p.id = vd.producto_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'PAGADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, true);
        sql.append(" GROUP BY 1 ORDER BY 1");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 2));
    }

    private ResultadoReporte comprobantesPorTipo(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("tipo", "Tipo"),
                ColumnaReporte.texto("estado", "Estado"),
                ColumnaReporte.entero("cantidad", "Cantidad"),
                ColumnaReporte.moneda("total", "Total"));
        StringBuilder sql = new StringBuilder(
                "SELECT c.tipo, c.estado, COUNT(*) cantidad, COALESCE(SUM(c.total), 0) total "
                + "FROM comprobante c WHERE c.empresa_id = ?");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroFecha(sql, p, "c.fecha_emision", f);
        if (f.sucursalId() != null) {
            sql.append(" AND c.sucursal_id = ?");
            p.add(f.sucursalId());
        }
        sql.append(" GROUP BY c.tipo, c.estado ORDER BY c.tipo, c.estado");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 3));
    }

    private ResultadoReporte impuestosPorTarifa(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.decimal("tarifa", "Tarifa %"),
                ColumnaReporte.entero("lineas", "Lineas"),
                ColumnaReporte.moneda("base", "Base imponible"),
                ColumnaReporte.moneda("valor", "Valor"));
        StringBuilder sql = new StringBuilder(
                "SELECT ci.tarifa, COUNT(*) lineas, COALESCE(SUM(ci.base_imponible), 0) base, "
                + "COALESCE(SUM(ci.valor), 0) valor "
                + "FROM comprobante_impuesto ci JOIN comprobante c ON c.id = ci.comprobante_id "
                + "WHERE c.empresa_id = ? AND c.estado <> 'ANULADO'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroFecha(sql, p, "c.fecha_emision", f);
        if (f.sucursalId() != null) {
            sql.append(" AND c.sucursal_id = ?");
            p.add(f.sucursalId());
        }
        sql.append(" GROUP BY ci.tarifa ORDER BY ci.tarifa");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 3));
    }

    // ------------------------------------------------------------------ operativo

    private ResultadoReporte sesionesDeCaja(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("caja", "Caja"),
                ColumnaReporte.texto("cajero", "Cajero"),
                ColumnaReporte.fecha("apertura", "Apertura"),
                ColumnaReporte.fecha("cierre", "Cierre"),
                ColumnaReporte.moneda("inicial", "Monto inicial"),
                ColumnaReporte.moneda("esperado", "Esperado"),
                ColumnaReporte.moneda("contado", "Contado"),
                ColumnaReporte.moneda("diferencia", "Diferencia"),
                ColumnaReporte.texto("estado", "Estado"));
        StringBuilder sql = new StringBuilder(
                "SELECT cj.nombre caja, "
                + "COALESCE(NULLIF(TRIM(COALESCE(ua.nombres, '') || ' ' || COALESCE(ua.apellidos, '')), ''), "
                + "'Sin usuario') cajero, sc.fecha_apertura::date apertura, sc.fecha_cierre::date cierre, "
                + "sc.monto_inicial inicial, COALESCE(sc.monto_esperado_efectivo, 0) esperado, "
                + "COALESCE(sc.monto_contado_efectivo, 0) contado, COALESCE(sc.diferencia, 0) diferencia, "
                + "sc.estado "
                + "FROM sesion_caja sc JOIN caja cj ON cj.id = sc.caja_id "
                + "LEFT JOIN usuario ua ON ua.id = sc.usuario_apertura_id "
                + "WHERE sc.empresa_id = ?");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        if (f.sucursalId() != null) {
            sql.append(" AND cj.sucursal_id = ?");
            p.add(f.sucursalId());
        }
        if (f.usuarioId() != null) {
            sql.append(" AND sc.usuario_apertura_id = ?");
            p.add(f.usuarioId());
        }
        filtroFecha(sql, p, "sc.fecha_apertura::date", f);
        sql.append(" ORDER BY sc.fecha_apertura DESC LIMIT 300");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, List.of());
    }

    private ResultadoReporte ventasPorCaja(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("caja", "Caja"),
                ColumnaReporte.entero("ventas", "Ventas"),
                ColumnaReporte.moneda("total", "Total"));
        StringBuilder sql = new StringBuilder(
                "SELECT cj.nombre caja, COUNT(*) ventas, COALESCE(SUM(v.total), 0) total "
                + "FROM venta v JOIN caja cj ON cj.id = v.caja_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'PAGADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, false);
        sql.append(" GROUP BY cj.id, cj.nombre ORDER BY total DESC");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, serieFilas(filas, 0, 2));
    }

    private ResultadoReporte devolucionesDeVenta(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.fecha("fecha", "Fecha"),
                ColumnaReporte.texto("numero", "Numero"),
                ColumnaReporte.texto("venta", "Venta"),
                ColumnaReporte.texto("cliente", "Cliente"),
                ColumnaReporte.moneda("total", "Total"),
                ColumnaReporte.texto("motivo", "Motivo"));
        StringBuilder sql = new StringBuilder(
                "SELECT dv.fecha::date fecha, dv.numero, v.numero venta, "
                + "COALESCE(NULLIF(TRIM(COALESCE(c.razon_social, '')), ''), "
                + "TRIM(COALESCE(c.nombres, '') || ' ' || COALESCE(c.apellidos, '')), 'Consumidor final') cliente, "
                + "dv.total, COALESCE(dv.motivo, '') motivo "
                + "FROM devolucion_venta dv JOIN venta v ON v.id = dv.venta_id "
                + "LEFT JOIN cliente c ON c.id = dv.cliente_id "
                + "WHERE dv.empresa_id = ? AND dv.estado <> 'ANULADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroFecha(sql, p, "dv.fecha::date", f);
        if (f.sucursalId() != null) {
            sql.append(" AND v.sucursal_id = ?");
            p.add(f.sucursalId());
        }
        if (f.usuarioId() != null) {
            sql.append(" AND dv.usuario_id = ?");
            p.add(f.usuarioId());
        }
        sql.append(" ORDER BY dv.fecha DESC LIMIT 300");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, List.of());
    }

    private ResultadoReporte anulacionesDeVenta(DefinicionReporte def, long empresaId, FiltroReporte f) {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.fecha("fecha", "Fecha"),
                ColumnaReporte.texto("numero", "Numero"),
                ColumnaReporte.texto("cliente", "Cliente"),
                ColumnaReporte.texto("usuario", "Usuario"),
                ColumnaReporte.moneda("total", "Total"));
        StringBuilder sql = new StringBuilder(
                "SELECT v.fecha::date fecha, v.numero, "
                + "COALESCE(NULLIF(TRIM(COALESCE(c.razon_social, '')), ''), "
                + "TRIM(COALESCE(c.nombres, '') || ' ' || COALESCE(c.apellidos, '')), 'Consumidor final') cliente, "
                + "COALESCE(NULLIF(TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellidos, '')), ''), "
                + "'Sin usuario') usuario, v.total "
                + "FROM venta v LEFT JOIN cliente c ON c.id = v.cliente_id "
                + "LEFT JOIN usuario u ON u.id = v.usuario_id "
                + "WHERE v.empresa_id = ? AND v.estado = 'ANULADA'");
        List<Object> p = new ArrayList<>(List.of(empresaId));
        filtroVenta(sql, p, "v", f, false);
        sql.append(" ORDER BY v.fecha DESC LIMIT 300");
        List<List<Object>> filas = consultar(sql.toString(), p, cols);
        return new ResultadoReporte(def, f, cols, filas, List.of());
    }

    // ------------------------------------------------------------------ helpers

    private List<SerieReporte> consultarBajoStock(Connection c, long empresaId, Long sucursalId, int umbral)
            throws SQLException {
        List<ColumnaReporte> cols = List.of(ColumnaReporte.texto("producto", "Producto"),
                ColumnaReporte.decimal("disponible", "Disponible"));
        StringBuilder sql = new StringBuilder(
                "SELECT p.nombre producto, COALESCE(SUM(s.cantidad - s.cantidad_reservada), 0) disponible "
                + "FROM producto p LEFT JOIN stock s ON s.producto_id = p.id "
                + "LEFT JOIN bodega b ON b.id = s.bodega_id "
                + "WHERE p.empresa_id = ? AND p.activo = TRUE AND p.tipo <> 'SERVICIO' "
                + "AND (CAST(? AS BIGINT) IS NULL OR b.sucursal_id = ?) "
                + "GROUP BY p.id, p.nombre "
                + "HAVING COALESCE(SUM(s.cantidad - s.cantidad_reservada), 0) <= ? AND COUNT(s.id) > 0 "
                + "ORDER BY disponible ASC, p.nombre LIMIT 200");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);
        params.add(sucursalId);
        params.add(sucursalId);
        params.add(umbral);
        return serie(c, sql.toString(), params, "producto", "disponible", true);
    }

    private static void filtroVenta(StringBuilder sql, List<Object> params, String alias,
                                    FiltroReporte f, boolean conCategoria) {
        filtroFecha(sql, params, alias + ".fecha::date", f);
        if (f.sucursalId() != null) {
            sql.append(" AND ").append(alias).append(".sucursal_id = ?");
            params.add(f.sucursalId());
        }
        if (f.usuarioId() != null) {
            sql.append(" AND ").append(alias).append(".usuario_id = ?");
            params.add(f.usuarioId());
        }
        if (conCategoria && f.categoriaId() != null) {
            sql.append(" AND p.categoria_id = ?");
            params.add(f.categoriaId());
        }
    }

    private static void filtroInventario(StringBuilder sql, List<Object> params, String aliasBodega,
                                         String aliasProducto, FiltroReporte f) {
        if (f.sucursalId() != null) {
            sql.append(" AND ").append(aliasBodega).append(".sucursal_id = ?");
            params.add(f.sucursalId());
        }
        if (f.categoriaId() != null) {
            sql.append(" AND ").append(aliasProducto).append(".categoria_id = ?");
            params.add(f.categoriaId());
        }
    }

    private static void filtroFecha(StringBuilder sql, List<Object> params, String expresion, FiltroReporte f) {
        if (f.desde() != null) {
            sql.append(" AND ").append(expresion).append(" >= ?");
            params.add(f.desde());
        }
        if (f.hasta() != null) {
            sql.append(" AND ").append(expresion).append(" <= ?");
            params.add(f.hasta());
        }
    }

    private List<List<Object>> consultar(String sql, List<Object> params, List<ColumnaReporte> columnas) {
        return Bd.consulta(c -> {
            List<List<Object>> filas = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                asignar(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        List<Object> fila = new ArrayList<>();
                        for (ColumnaReporte columna : columnas) {
                            fila.add(leer(rs, columna));
                        }
                        filas.add(fila);
                    }
                }
            }
            return filas;
        });
    }

    private static Object leer(ResultSet rs, ColumnaReporte columna) throws SQLException {
        return switch (columna.tipo()) {
            case TEXTO -> rs.getString(columna.clave());
            case ENTERO -> rs.getLong(columna.clave());
            case DECIMAL, MONEDA, PORCENTAJE -> rs.getBigDecimal(columna.clave());
            case FECHA -> rs.getObject(columna.clave(), LocalDate.class);
        };
    }

    private List<SerieReporte> serie(Connection c, String sql, List<Object> params,
                                     String etiqueta, String valor, boolean saltarCero) throws SQLException {
        List<SerieReporte> serie = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal monto = rs.getBigDecimal(valor);
                    if (saltarCero && monto != null && monto.signum() == 0) {
                        continue;
                    }
                    serie.add(new SerieReporte(rs.getString(etiqueta), monto == null ? BigDecimal.ZERO : monto));
                }
            }
        }
        return serie;
    }

    private static List<SerieReporte> serieFilas(List<List<Object>> filas, int etiqueta, int valor) {
        List<SerieReporte> serie = new ArrayList<>();
        for (List<Object> fila : filas) {
            if (etiqueta >= fila.size() || valor >= fila.size()) {
                continue;
            }
            Object v = fila.get(valor);
            BigDecimal monto = v instanceof BigDecimal bd ? bd
                    : v instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
            if (monto.signum() == 0) {
                continue;
            }
            Object e = fila.get(etiqueta);
            serie.add(new SerieReporte(e == null ? "" : FormatoReporte.valor(e,
                    e instanceof LocalDate ? TipoColumna.FECHA : TipoColumna.TEXTO), monto));
        }
        return serie;
    }

    private static BigDecimal escalar(Connection c, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    private static long contar(Connection c, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private static void asignar(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
