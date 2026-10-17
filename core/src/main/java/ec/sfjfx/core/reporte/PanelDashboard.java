package ec.sfjfx.core.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Datos del panel de inicio (KPIs y series de los graficos). */
public final class PanelDashboard {

    private final LocalDate fecha;
    private final BigDecimal ventasHoy;
    private final BigDecimal ticketPromedio;
    private final BigDecimal utilidadHoy;
    private final BigDecimal cuentasPorCobrar;
    private final BigDecimal cuentasPorPagar;
    private final long cantidadVentasHoy;
    private final long productosBajoStock;
    private final long sesionesAbiertas;
    private final List<SerieReporte> ventasUltimosDias;
    private final List<SerieReporte> mediosDePago;
    private final List<SerieReporte> topProductos;

    public PanelDashboard(LocalDate fecha, BigDecimal ventasHoy, BigDecimal ticketPromedio,
                          BigDecimal utilidadHoy, BigDecimal cuentasPorCobrar, BigDecimal cuentasPorPagar,
                          long cantidadVentasHoy, long productosBajoStock, long sesionesAbiertas,
                          List<SerieReporte> ventasUltimosDias, List<SerieReporte> mediosDePago,
                          List<SerieReporte> topProductos) {
        this.fecha = fecha;
        this.ventasHoy = ventasHoy;
        this.ticketPromedio = ticketPromedio;
        this.utilidadHoy = utilidadHoy;
        this.cuentasPorCobrar = cuentasPorCobrar;
        this.cuentasPorPagar = cuentasPorPagar;
        this.cantidadVentasHoy = cantidadVentasHoy;
        this.productosBajoStock = productosBajoStock;
        this.sesionesAbiertas = sesionesAbiertas;
        this.ventasUltimosDias = ventasUltimosDias;
        this.mediosDePago = mediosDePago;
        this.topProductos = topProductos;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public BigDecimal ventasHoy() {
        return ventasHoy;
    }

    public BigDecimal ticketPromedio() {
        return ticketPromedio;
    }

    public BigDecimal utilidadHoy() {
        return utilidadHoy;
    }

    public BigDecimal cuentasPorCobrar() {
        return cuentasPorCobrar;
    }

    public BigDecimal cuentasPorPagar() {
        return cuentasPorPagar;
    }

    public long cantidadVentasHoy() {
        return cantidadVentasHoy;
    }

    public long productosBajoStock() {
        return productosBajoStock;
    }

    public long sesionesAbiertas() {
        return sesionesAbiertas;
    }

    public List<SerieReporte> ventasUltimosDias() {
        return ventasUltimosDias;
    }

    public List<SerieReporte> mediosDePago() {
        return mediosDePago;
    }

    public List<SerieReporte> topProductos() {
        return topProductos;
    }
}
