package ec.sfjfx.core.reporte;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Resultado de ejecutar un reporte: columnas, filas, totales y serie para el grafico. */
public final class ResultadoReporte {

    private final DefinicionReporte definicion;
    private final FiltroReporte filtro;
    private final List<ColumnaReporte> columnas;
    private final List<List<Object>> filas;
    private final List<BigDecimal> totales;
    private final List<SerieReporte> serie;

    public ResultadoReporte(DefinicionReporte definicion, FiltroReporte filtro,
                            List<ColumnaReporte> columnas, List<List<Object>> filas,
                            List<SerieReporte> serie) {
        this.definicion = definicion;
        this.filtro = filtro;
        this.columnas = List.copyOf(columnas);
        this.filas = List.copyOf(filas);
        this.serie = serie == null ? List.of() : List.copyOf(serie);
        this.totales = calcularTotales(columnas, filas);
    }

    private static List<BigDecimal> calcularTotales(List<ColumnaReporte> columnas, List<List<Object>> filas) {
        List<BigDecimal> resultado = new ArrayList<>();
        for (int i = 0; i < columnas.size(); i++) {
            ColumnaReporte columna = columnas.get(i);
            if (!columna.totaliza()) {
                resultado.add(null);
                continue;
            }
            BigDecimal suma = BigDecimal.ZERO;
            for (List<Object> fila : filas) {
                Object valor = i < fila.size() ? fila.get(i) : null;
                if (valor instanceof BigDecimal bd) {
                    suma = suma.add(bd);
                } else if (valor instanceof Number n) {
                    suma = suma.add(BigDecimal.valueOf(n.doubleValue()));
                }
            }
            resultado.add(suma);
        }
        return resultado;
    }

    public DefinicionReporte definicion() {
        return definicion;
    }

    public String codigo() {
        return definicion.codigo();
    }

    public FiltroReporte filtro() {
        return filtro;
    }

    public List<ColumnaReporte> columnas() {
        return columnas;
    }

    public List<List<Object>> filas() {
        return filas;
    }

    public List<BigDecimal> totales() {
        return totales;
    }

    public List<SerieReporte> serie() {
        return serie;
    }

    public boolean vacio() {
        return filas.isEmpty();
    }
}
