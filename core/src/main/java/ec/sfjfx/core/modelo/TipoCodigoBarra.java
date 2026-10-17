package ec.sfjfx.core.modelo;

/** Tipo de codigo de barras. */
public enum TipoCodigoBarra {
    EAN13("EAN-13"),
    UPC_A("UPC-A"),
    EAN8("EAN-8"),
    DUN14("DUN-14"),
    INTERNO("Interno"),
    PLU("PLU (granel)");

    private final String etiqueta;

    TipoCodigoBarra(String etiqueta) {
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
