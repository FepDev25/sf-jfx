package ec.sfjfx.core.modelo;

/** Proveedor. */
public record Proveedor(
        Long id,
        long empresaId,
        String tipoIdentificacion,
        String numeroIdentificacion,
        String razonSocial,
        String nombreComercial,
        String direccion,
        String telefono,
        String email,
        String tipoContribuyente,
        int diasCredito,
        boolean aplicaRetencionIva,
        boolean aplicaRetencionRenta,
        boolean activo,
        String nota) {

    @Override
    public String toString() {
        return razonSocial;
    }
}
