package ec.sfjfx.core.servicio;

/** Tipos de importacion masiva soportados (el catalogo ya tiene su propio importador). */
public enum TipoImportacion {

    CLIENTES("Clientes", new String[]{
            "tipo", "tipo_identificacion", "numero_identificacion", "nombres", "apellidos",
            "razon_social", "email", "telefono", "direccion", "es_credito", "limite_credito",
            "dias_credito"}),
    PROVEEDORES("Proveedores", new String[]{
            "tipo_identificacion", "numero_identificacion", "razon_social", "nombre_comercial",
            "direccion", "telefono", "email", "tipo_contribuyente", "dias_credito",
            "aplica_retencion_iva", "aplica_retencion_renta", "nota"});

    private final String etiqueta;
    private final String[] cabecera;

    TipoImportacion(String etiqueta, String[] cabecera) {
        this.etiqueta = etiqueta;
        this.cabecera = cabecera;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String[] cabecera() {
        return cabecera.clone();
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
