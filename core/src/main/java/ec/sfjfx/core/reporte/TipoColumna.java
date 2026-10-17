package ec.sfjfx.core.reporte;

/** Tipo de dato de una columna de reporte. */
public enum TipoColumna {

    TEXTO,
    ENTERO,
    DECIMAL,
    MONEDA,
    PORCENTAJE,
    FECHA;

    public boolean esNumerica() {
        return this == ENTERO || this == DECIMAL || this == MONEDA || this == PORCENTAJE;
    }
}
