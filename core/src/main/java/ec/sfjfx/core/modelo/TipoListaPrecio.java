package ec.sfjfx.core.modelo;

/** Tipo de lista de precios. */
public enum TipoListaPrecio {
    PUBLICO("Publico"),
    MAYORISTA("Mayorista"),
    CORPORATIVO("Corporativo"),
    COSTO("Costo");

    private final String etiqueta;

    TipoListaPrecio(String etiqueta) {
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
