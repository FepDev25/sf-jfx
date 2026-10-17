package ec.sfjfx.core.reporte;

/** Definicion de un reporte disponible en el catalogo. */
public record DefinicionReporte(String codigo, String nombre, GrupoReporte grupo,
                                String descripcion, TipoGrafico grafico) {

    public boolean tieneGrafico() {
        return grafico != null && grafico != TipoGrafico.NINGUNO;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
