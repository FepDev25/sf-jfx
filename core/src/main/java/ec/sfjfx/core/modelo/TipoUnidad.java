package ec.sfjfx.core.modelo;

/** Tipo de unidad de medida. */
public enum TipoUnidad {
    UNIDAD("Unidad"),
    PESO("Peso"),
    VOLUMEN("Volumen"),
    LONGITUD("Longitud");

    private final String etiqueta;

    TipoUnidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
