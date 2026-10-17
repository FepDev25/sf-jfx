package ec.sfjfx.ui.vistas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ec.sfjfx.core.reporte.FormatoReporte;
import ec.sfjfx.core.reporte.PanelDashboard;
import ec.sfjfx.core.reporte.ServicioReportes;
import ec.sfjfx.core.reporte.TipoGrafico;
import ec.sfjfx.core.seguridad.SesionActual;
import ec.sfjfx.ui.componentes.GraficoSeries;
import ec.sfjfx.ui.util.Alertas;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Panel de inicio con KPIs en vivo y graficos. */
public final class VistaDashboard extends BorderPane {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ServicioReportes reportes = new ServicioReportes();
    private final VBox contenido = new VBox(18);
    private final Label actualizado = new Label();

    public VistaDashboard() {
        getStyleClass().add("contenido");
        setPadding(new Insets(20));
        setTop(construirCabecera());
        contenido.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        setCenter(scroll);

        cargar();
    }

    private Node construirCabecera() {
        Label titulo = new Label("Panel de control");
        titulo.getStyleClass().add("titulo-vista");
        actualizado.getStyleClass().add("texto-suave");

        Button refrescar = new Button("Actualizar");
        refrescar.getStyleClass().add("boton-secundario");
        refrescar.setOnAction(e -> cargar());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox cabecera = new HBox(12, new VBox(2, titulo, actualizado), espacio, refrescar);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setPadding(new Insets(0, 0, 14, 0));
        return cabecera;
    }

    private void cargar() {
        try {
            PanelDashboard panel = reportes.dashboard();
            contenido.getChildren().setAll(
                    tarjetas(panel),
                    graficos(panel),
                    pieIndicadores(panel));
            String sucursal = SesionActual.requerida().sucursal() == null
                    ? "Todas las sucursales"
                    : SesionActual.requerida().sucursal().nombre();
            actualizado.setText(sucursal + " · actualizado " + LocalDateTime.now().format(HORA));
        } catch (RuntimeException e) {
            contenido.getChildren().setAll(new Label("No se pudieron cargar los indicadores: " + e.getMessage()));
            Alertas.error("Panel de control", e.getMessage());
        }
    }

    private Node tarjetas(PanelDashboard panel) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(25);
            grid.getColumnConstraints().add(columna);
        }
        grid.add(tarjeta("Ventas de hoy", FormatoReporte.moneda(panel.ventasHoy()),
                panel.cantidadVentasHoy() + " ventas registradas", false), 0, 0);
        grid.add(tarjeta("Ticket promedio", FormatoReporte.moneda(panel.ticketPromedio()),
                "Promedio de hoy", false), 1, 0);
        grid.add(tarjeta("Utilidad de hoy", FormatoReporte.moneda(panel.utilidadHoy()),
                "Ventas menos costo", false), 2, 0);
        grid.add(tarjeta("Cajas abiertas", String.valueOf(panel.sesionesAbiertas()),
                "Turnos en curso", false), 3, 0);
        grid.add(tarjeta("Cuentas por cobrar", FormatoReporte.moneda(panel.cuentasPorCobrar()),
                "Saldo pendiente de clientes", false), 0, 1);
        grid.add(tarjeta("Cuentas por pagar", FormatoReporte.moneda(panel.cuentasPorPagar()),
                "Facturas de proveedor pendientes", false), 1, 1);
        grid.add(tarjeta("Stock bajo", String.valueOf(panel.productosBajoStock()),
                "Productos en el umbral o menos", panel.productosBajoStock() > 0), 2, 1);
        grid.add(tarjeta("Ventas últimos 7 días", FormatoReporte.moneda(
                panel.ventasUltimosDias().stream().map(ec.sfjfx.core.reporte.SerieReporte::valor)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)),
                "Total acumulado", false), 3, 1);
        return grid;
    }

    private VBox tarjeta(String titulo, String valor, String detalle, boolean alerta) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("kpi-titulo");
        Label numero = new Label(valor);
        numero.getStyleClass().add("kpi-valor");
        if (alerta) {
            numero.getStyleClass().add("kpi-valor-alerta");
        }
        Label pie = new Label(detalle);
        pie.getStyleClass().add("kpi-detalle");
        pie.setWrapText(true);
        VBox tarjeta = new VBox(4, etiqueta, numero, pie);
        tarjeta.getStyleClass().add("panel-tarjeta");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private Node graficos(PanelDashboard panel) {
        VBox ventas = tarjetaGrafico("Ventas de los últimos 7 días",
                GraficoSeries.crear(TipoGrafico.LINEAS, panel.ventasUltimosDias(), ""));
        VBox medios = tarjetaGrafico("Medios de pago (30 días)",
                GraficoSeries.crear(TipoGrafico.PASTEL, panel.mediosDePago(), ""));
        VBox productos = tarjetaGrafico("Top productos (30 días)",
                GraficoSeries.crear(TipoGrafico.BARRAS, panel.topProductos(), ""));

        HBox fila = new HBox(12, ventas, medios, productos);
        HBox.setHgrow(ventas, Priority.ALWAYS);
        HBox.setHgrow(medios, Priority.ALWAYS);
        HBox.setHgrow(productos, Priority.ALWAYS);
        ventas.setMaxWidth(Double.MAX_VALUE);
        medios.setMaxWidth(Double.MAX_VALUE);
        productos.setMaxWidth(Double.MAX_VALUE);
        return fila;
    }

    private VBox tarjetaGrafico(String titulo, Node grafico) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("panel-titulo");
        if (grafico instanceof javafx.scene.chart.Chart chart) {
            chart.setPrefHeight(250);
            chart.setMinHeight(250);
        }
        VBox caja = new VBox(8, etiqueta, grafico);
        caja.getStyleClass().add("panel-tarjeta");
        VBox.setVgrow(grafico, Priority.ALWAYS);
        return caja;
    }

    private Node pieIndicadores(PanelDashboard panel) {
        Label texto = new Label("Los reportes detallados están disponibles en el módulo Reportes. "
                + "Use los filtros por fecha, sucursal, categoría y cajero y exporte a CSV, Excel o PDF.");
        texto.getStyleClass().add("texto-suave");
        texto.setWrapText(true);
        VBox caja = new VBox(texto);
        caja.getStyleClass().add("panel-tarjeta");
        return caja;
    }
}
