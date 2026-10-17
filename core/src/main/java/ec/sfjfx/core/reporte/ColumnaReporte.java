package ec.sfjfx.core.reporte;

/** Columna de un reporte, con su tipo y si participa del total. */
public record ColumnaReporte(String clave, String titulo, TipoColumna tipo, boolean totaliza) {

    public ColumnaReporte(String clave, String titulo, TipoColumna tipo) {
        this(clave, titulo, tipo, false);
    }

    public static ColumnaReporte texto(String clave, String titulo) {
        return new ColumnaReporte(clave, titulo, TipoColumna.TEXTO);
    }

    public static ColumnaReporte entero(String clave, String titulo) {
        return new ColumnaReporte(clave, titulo, TipoColumna.ENTERO, true);
    }

    public static ColumnaReporte decimal(String clave, String titulo) {
        return new ColumnaReporte(clave, titulo, TipoColumna.DECIMAL, true);
    }

    public static ColumnaReporte moneda(String clave, String titulo) {
        return new ColumnaReporte(clave, titulo, TipoColumna.MONEDA, true);
    }

    public static ColumnaReporte porcentaje(String clave, String titulo) {
        return new ColumnaReporte(clave, titulo, TipoColumna.PORCENTAJE);
    }

    public static ColumnaReporte fecha(String clave, String titulo) {
        return new ColumnaReporte(clave, titulo, TipoColumna.FECHA);
    }
}
