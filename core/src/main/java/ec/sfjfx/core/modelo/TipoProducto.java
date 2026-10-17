package ec.sfjfx.core.modelo;

/** Tipo de producto. */
public enum TipoProducto {
    SIMPLE("Producto simple"),
    VARIANTE_PADRE("Producto con variantes"),
    VARIANTE("Variante"),
    KIT("Kit / armado"),
    SERVICIO("Servicio"),
    GRANEL("Granel / pesable");

    private final String etiqueta;

    TipoProducto(String etiqueta) {
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
