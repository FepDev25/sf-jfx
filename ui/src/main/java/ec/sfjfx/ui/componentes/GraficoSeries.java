package ec.sfjfx.ui.componentes;

import java.math.BigDecimal;
import java.util.List;

import ec.sfjfx.core.reporte.SerieReporte;
import ec.sfjfx.core.reporte.TipoGrafico;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

/** Construye graficos JavaFX (barras, lineas, pastel) a partir de una serie de reporte. */
public final class GraficoSeries {

    private GraficoSeries() {
    }

    public static Node crear(TipoGrafico tipo, List<SerieReporte> serie, String titulo) {
        if (tipo == null || tipo == TipoGrafico.NINGUNO || serie == null || serie.isEmpty()) {
            Label vacio = new Label("Sin datos para graficar");
            vacio.getStyleClass().add("texto-suave");
            return vacio;
        }
        return switch (tipo) {
            case LINEAS -> lineas(serie, titulo);
            case PASTEL -> pastel(serie, titulo);
            case BARRAS -> barras(serie, titulo);
            case NINGUNO -> new Label("Sin datos para graficar");
        };
    }

    private static Node lineas(List<SerieReporte> serie, String titulo) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        LineChart<String, Number> grafico = new LineChart<>(x, y);
        grafico.setTitle(titulo);
        grafico.setLegendVisible(false);
        grafico.setAnimated(false);
        XYChart.Series<String, Number> datos = new XYChart.Series<>();
        for (SerieReporte punto : serie) {
            datos.getData().add(new XYChart.Data<>(punto.etiqueta(), punto.valor()));
        }
        grafico.getData().add(datos);
        grafico.getStyleClass().add("grafico-reporte");
        return grafico;
    }

    private static Node barras(List<SerieReporte> serie, String titulo) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> grafico = new BarChart<>(x, y);
        grafico.setTitle(titulo);
        grafico.setLegendVisible(false);
        grafico.setAnimated(false);
        XYChart.Series<String, Number> datos = new XYChart.Series<>();
        for (SerieReporte punto : serie) {
            datos.getData().add(new XYChart.Data<>(punto.etiqueta(), punto.valor()));
        }
        grafico.getData().add(datos);
        x.setTickLabelRotation(-25);
        grafico.getStyleClass().add("grafico-reporte");
        return grafico;
    }

    private static Node pastel(List<SerieReporte> serie, String titulo) {
        PieChart grafico = new PieChart();
        grafico.setTitle(titulo);
        grafico.setAnimated(false);
        grafico.setLegendSide(Side.BOTTOM);
        BigDecimal total = serie.stream().map(SerieReporte::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (SerieReporte punto : serie) {
            if (punto.valor() == null || punto.valor().signum() <= 0) {
                continue;
            }
            double porcentaje = total.signum() == 0 ? 0
                    : punto.valor().doubleValue() * 100.0 / total.doubleValue();
            String etiqueta = String.format("%s (%.1f%%)", punto.etiqueta(), porcentaje);
            grafico.getData().add(new PieChart.Data(etiqueta, punto.valor().doubleValue()));
        }
        grafico.getStyleClass().add("grafico-reporte");
        return grafico;
    }
}
