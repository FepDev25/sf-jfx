package ec.sfjfx.core.modelo;

/** Tipo de impuesto. */
public enum TipoImpuesto {
    IVA("IVA"),
    ICE("ICE"),
    IRBPNR("IRBPNR"),
    OTRO("Otro");

    private final String etiqueta;

    TipoImpuesto(String etiqueta) {
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
