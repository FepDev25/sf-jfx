package ec.sfjfx.core.modelo;

/** Tipo de dato de un atributo personalizado. */
public enum TipoDatoAtributo {
    TEXTO("Texto"),
    NUMERO("Numero"),
    DECIMAL("Decimal"),
    BOOLEAN("Si/No"),
    FECHA("Fecha"),
    LISTA("Lista");

    private final String etiqueta;

    TipoDatoAtributo(String etiqueta) {
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
