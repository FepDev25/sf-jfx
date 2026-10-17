package ec.sfjfx.core.reporte;

import java.util.List;
import java.util.Optional;

/** Catalogo de reportes disponibles. */
public final class CatalogoReportes {

    private static final List<DefinicionReporte> TODOS = List.of(
            new DefinicionReporte("ventas_por_dia", "Ventas por dia", GrupoReporte.VENTAS,
                    "Total vendido, impuestos y utilidad agrupados por fecha.", TipoGrafico.LINEAS),
            new DefinicionReporte("ventas_por_cajero", "Ventas por cajero", GrupoReporte.VENTAS,
                    "Rendimiento de cada cajero: numero de ventas, total y ticket promedio.", TipoGrafico.BARRAS),
            new DefinicionReporte("ventas_por_producto", "Ventas por producto", GrupoReporte.VENTAS,
                    "Productos mas vendidos por cantidad e ingresos generados.", TipoGrafico.BARRAS),
            new DefinicionReporte("ventas_por_categoria", "Ventas por categoria", GrupoReporte.VENTAS,
                    "Participacion de cada categoria en las ventas.", TipoGrafico.BARRAS),
            new DefinicionReporte("ventas_por_medio_pago", "Ventas por medio de pago", GrupoReporte.VENTAS,
                    "Distribucion de los cobros segun el medio de pago.", TipoGrafico.PASTEL),

            new DefinicionReporte("stock_valorizado", "Stock valorizado", GrupoReporte.INVENTARIO,
                    "Existencias por producto y bodega valuadas al costo promedio.", TipoGrafico.BARRAS),
            new DefinicionReporte("stock_bajo", "Productos con stock bajo", GrupoReporte.INVENTARIO,
                    "Productos cuya existencia disponible esta en el umbral o por debajo.", TipoGrafico.BARRAS),
            new DefinicionReporte("lotes_por_vencer", "Lotes por vencer", GrupoReporte.INVENTARIO,
                    "Lotes con existencias y fecha de vencimiento proxima.", TipoGrafico.NINGUNO),
            new DefinicionReporte("kardex_por_tipo", "Movimientos por tipo", GrupoReporte.INVENTARIO,
                    "Resumen del kardex agrupado por tipo de movimiento.", TipoGrafico.PASTEL),

            new DefinicionReporte("compras_por_proveedor", "Compras por proveedor", GrupoReporte.COMPRAS,
                    "Facturas de proveedor, impuestos y saldos pendientes por proveedor.", TipoGrafico.BARRAS),
            new DefinicionReporte("cuentas_por_pagar", "Cuentas por pagar", GrupoReporte.COMPRAS,
                    "Facturas de proveedor con saldo pendiente y vencimientos.", TipoGrafico.NINGUNO),
            new DefinicionReporte("pagos_a_proveedores", "Pagos a proveedores", GrupoReporte.COMPRAS,
                    "Pagos realizados con su medio y referencia.", TipoGrafico.NINGUNO),

            new DefinicionReporte("cuentas_por_cobrar", "Cuentas por cobrar", GrupoReporte.FINANCIERO,
                    "Saldo pendiente por cliente con cargos y abonos del periodo.", TipoGrafico.BARRAS),
            new DefinicionReporte("utilidad_por_dia", "Utilidad por dia", GrupoReporte.FINANCIERO,
                    "Ventas, costo, utilidad y margen diario.", TipoGrafico.LINEAS),
            new DefinicionReporte("abonos_de_clientes", "Abonos de clientes", GrupoReporte.FINANCIERO,
                    "Pagos recibidos de clientes a cuenta de sus saldos.", TipoGrafico.NINGUNO),

            new DefinicionReporte("iva_por_dia", "IVA generado por dia", GrupoReporte.IMPUESTOS,
                    "Base imponible e IVA de las ventas agrupados por fecha.", TipoGrafico.LINEAS),
            new DefinicionReporte("comprobantes_por_tipo", "Comprobantes emitidos", GrupoReporte.IMPUESTOS,
                    "Comprobantes electronicos agrupados por tipo y estado.", TipoGrafico.BARRAS),
            new DefinicionReporte("impuestos_por_tarifa", "Impuestos por tarifa", GrupoReporte.IMPUESTOS,
                    "Base imponible y valor recaudado por tarifa de impuesto.", TipoGrafico.PASTEL),

            new DefinicionReporte("sesiones_de_caja", "Sesiones de caja", GrupoReporte.OPERATIVO,
                    "Turnos de caja con montos de apertura, arqueo y diferencias.", TipoGrafico.NINGUNO),
            new DefinicionReporte("ventas_por_caja", "Ventas por caja", GrupoReporte.OPERATIVO,
                    "Totales cobrados por cada terminal de caja.", TipoGrafico.BARRAS),
            new DefinicionReporte("devoluciones_de_venta", "Devoluciones de venta", GrupoReporte.OPERATIVO,
                    "Devoluciones registradas con su venta, cliente y motivo.", TipoGrafico.NINGUNO),
            new DefinicionReporte("anulaciones_de_venta", "Anulaciones de venta", GrupoReporte.OPERATIVO,
                    "Ventas anuladas en el periodo con su usuario y total.", TipoGrafico.NINGUNO));

    private CatalogoReportes() {
    }

    public static List<DefinicionReporte> todos() {
        return TODOS;
    }

    public static List<DefinicionReporte> porGrupo(GrupoReporte grupo) {
        return TODOS.stream().filter(r -> r.grupo() == grupo).toList();
    }

    public static Optional<DefinicionReporte> porCodigo(String codigo) {
        return TODOS.stream().filter(r -> r.codigo().equals(codigo)).findFirst();
    }
}
