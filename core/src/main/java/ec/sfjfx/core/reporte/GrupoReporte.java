package ec.sfjfx.core.reporte;

/** Grupos de reporteria del sistema. */
public enum GrupoReporte {

    VENTAS("Ventas / POS"),
    INVENTARIO("Inventario"),
    COMPRAS("Compras / Proveedores"),
    FINANCIERO("Financiero"),
    IMPUESTOS("Impuestos / SRI"),
    OPERATIVO("Operativo / Caja");

    private final String etiqueta;

    GrupoReporte(String etiqueta) {
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
